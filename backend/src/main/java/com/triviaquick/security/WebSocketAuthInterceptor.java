package com.triviaquick.security;

import com.triviaquick.service.SessionAccessService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autorización del canal STOMP:
 * <ul>
 *   <li>CONNECT exige una identidad válida (cookie del handshake o cabecera Authorization).</li>
 *   <li>SUBSCRIBE solo a {@code /topic/sessions/{id}} y solo si se es anfitrión o participante de esa sesión.</li>
 *   <li>SEND se rechaza siempre: los clientes no pueden enviar mensajes; las puntuaciones y resultados
 *   oficiales solo los publica el servidor.</li>
 * </ul>
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    public static final String USER_ATTRIBUTE = "tq.user";
    private static final Pattern SESSION_TOPIC = Pattern.compile("^/topic/sessions/(\\d{1,18})$");

    private final TokenResolver tokenResolver;
    private final SessionAccessService access;

    public WebSocketAuthInterceptor(TokenResolver tokenResolver, SessionAccessService access) {
        this.tokenResolver = tokenResolver;
        this.access = access;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        switch (command) {
            case CONNECT, STOMP -> {
                AuthUser user = resolveUser(accessor)
                        .orElseThrow(() -> new MessageDeliveryException("No autenticado"));
                accessor.setUser(new StompPrincipal(user));
            }
            case SUBSCRIBE -> {
                AuthUser user = currentUser(accessor);
                String destination = accessor.getDestination();
                Matcher m = destination == null ? null : SESSION_TOPIC.matcher(destination);
                if (m == null || !m.matches() || !access.canView(Long.parseLong(m.group(1)), user.id())) {
                    throw new MessageDeliveryException("Suscripción no autorizada");
                }
            }
            case SEND -> throw new MessageDeliveryException("Los clientes no pueden enviar mensajes por este canal");
            default -> {
                // DISCONNECT, UNSUBSCRIBE, ACK, NACK: no requieren comprobaciones adicionales.
            }
        }
        return message;
    }

    private Optional<AuthUser> resolveUser(StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs != null && attrs.get(USER_ATTRIBUTE) instanceof AuthUser user) {
            return Optional.of(user);
        }
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return tokenResolver.authenticate(header.substring(7).trim());
        }
        return Optional.empty();
    }

    private AuthUser currentUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof StompPrincipal p) {
            return p.user();
        }
        throw new MessageDeliveryException("No autenticado");
    }

    /** Principal del canal STOMP; envuelve la identidad autenticada. */
    public record StompPrincipal(AuthUser user) implements java.security.Principal {
        @Override
        public String getName() {
            return String.valueOf(user.id());
        }
    }
}
