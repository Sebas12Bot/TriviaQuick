package com.triviaquick.config;

import com.triviaquick.security.TokenResolver;
import com.triviaquick.security.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * STOMP sobre WebSocket. El servidor publica en {@code /topic/sessions/{id}}; no hay destinos
 * de aplicación para mensajes del cliente (todo SEND se rechaza en {@link WebSocketAuthInterceptor}).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AppProperties props;
    private final TokenResolver tokenResolver;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(AppProperties props, TokenResolver tokenResolver, WebSocketAuthInterceptor authInterceptor) {
        this.props = props;
        this.tokenResolver = tokenResolver;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // Solo los orígenes autorizados pueden abrir el WebSocket (protección contra CSWSH).
                .setAllowedOrigins(props.cors().allowedOrigins().stream().map(String::trim).toArray(String[]::new))
                .addInterceptors(new CookieHandshakeInterceptor());
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    /** Valida la cookie de sesión durante el handshake y guarda la identidad para el CONNECT. */
    private final class CookieHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servlet) {
                tokenResolver.cookieValue(servlet.getServletRequest().getCookies())
                        .flatMap(tokenResolver::authenticate)
                        .ifPresent(user -> attributes.put(WebSocketAuthInterceptor.USER_ATTRIBUTE, user));
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}
