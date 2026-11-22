package com.triviaquick.security;

import com.triviaquick.config.AppProperties;
import com.triviaquick.model.User;
import com.triviaquick.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Obtiene y valida el token de una petición (cookie httpOnly o cabecera Authorization)
 * y lo convierte en un {@link AuthUser} comprobando que la cuenta exista y esté activa.
 */
@Component
public class TokenResolver {

    private final JwtService jwtService;
    private final UserRepository users;
    private final AppProperties.Cookie cookieProps;

    public TokenResolver(JwtService jwtService, UserRepository users, AppProperties props) {
        this.jwtService = jwtService;
        this.users = users;
        this.cookieProps = props.cookie();
    }

    public Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7).trim());
        }
        return cookieValue(request.getCookies());
    }

    public Optional<String> cookieValue(Cookie[] cookies) {
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie c : cookies) {
            if (cookieProps.name().equals(c.getName())) {
                return Optional.ofNullable(c.getValue());
            }
        }
        return Optional.empty();
    }

    public boolean hasSessionCookie(HttpServletRequest request) {
        return cookieValue(request.getCookies()).isPresent();
    }

    /**
     * Valida el token y la cuenta: debe existir, estar activa y el token debe ser posterior al último
     * cambio de credenciales (cambiar la contraseña o eliminar la cuenta cierra las demás sesiones).
     */
    public Optional<AuthUser> authenticate(String token) {
        return jwtService.parse(token).flatMap(claims -> users.findById(claims.userId())
                .filter(User::isActive)
                .filter(u -> u.getCredentialsChangedAt() == null
                        || !claims.issuedAt().isBefore(u.getCredentialsChangedAt().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))))
                .map(u -> new AuthUser(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole()));
    }

    public ResponseCookie sessionCookie(String token) {
        return baseCookie(token).maxAge(jwtService.ttl()).build();
    }

    public ResponseCookie clearedCookie() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieProps.name(), value)
                .httpOnly(true)
                .secure(cookieProps.secure())
                .sameSite(cookieProps.sameSite())
                .path("/");
    }
}
