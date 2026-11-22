package com.triviaquick.security;

import com.triviaquick.model.Role;
import com.triviaquick.service.SessionAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private final AuthUser student = new AuthUser(5L, "e@x.com", "Estudiante", Role.STUDENT);
    private TokenResolver tokens;
    private SessionAccessService access;
    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        tokens = mock(TokenResolver.class);
        access = mock(SessionAccessService.class);
        when(tokens.authenticate(anyString())).thenReturn(Optional.empty());
        interceptor = new WebSocketAuthInterceptor(tokens, access);
    }

    private Message<byte[]> frame(StompCommand command, String destination, boolean authenticated) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        Map<String, Object> attrs = new HashMap<>();
        if (authenticated) {
            attrs.put(WebSocketAuthInterceptor.USER_ATTRIBUTE, student);
            accessor.setUser(new WebSocketAuthInterceptor.StompPrincipal(student));
        }
        accessor.setSessionAttributes(attrs);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connectWithoutIdentityIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, false), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void connectWithHandshakeIdentityIsAccepted() {
        assertThat(interceptor.preSend(frame(StompCommand.CONNECT, null, true), null)).isNotNull();
    }

    @Test
    void clientSendIsAlwaysRejectedEvenForMembers() {
        when(access.canView(1L, 5L)).thenReturn(true);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, "/app/sessions/1/score", true), null))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, "/topic/sessions/1", true), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscribeToForeignSessionIsRejected() {
        when(access.canView(2L, 5L)).thenReturn(false);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/sessions/2", true), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscribeToOwnSessionIsAccepted() {
        when(access.canView(1L, 5L)).thenReturn(true);
        assertThat(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/sessions/1", true), null)).isNotNull();
    }

    @Test
    void subscribeToUnknownDestinationIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/sessions/1/../2", true), null))
                .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/everything", true), null))
                .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void subscribeWithoutIdentityIsRejected() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/sessions/1", false), null))
                .isInstanceOf(MessageDeliveryException.class);
    }
}
