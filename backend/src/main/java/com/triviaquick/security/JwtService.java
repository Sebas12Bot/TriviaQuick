package com.triviaquick.security;

import com.triviaquick.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ISSUER = "triviaquick";

    private final SecretKey key;
    private final Duration ttl;
    private final Clock clock;

    public JwtService(AppProperties props, Clock clock) {
        this.clock = clock;
        this.ttl = Duration.ofMinutes(props.jwt().expirationMinutes());
        String secret = props.jwt().secret();
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            this.key = Keys.hmacShaKeyFor(random);
            log.warn("JWT_SECRET no está definido: se usa una clave aleatoria temporal (solo desarrollo). "
                    + "Las sesiones se invalidan al reiniciar. Define JWT_SECRET en producción.");
        } else {
            byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET debe tener al menos 32 bytes");
            }
            this.key = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String issue(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Emite un token para una sesión renovada tras un cambio de credenciales en {@code changedAt}. */
    public String issueAfter(Long userId, Instant changedAt) {
        return issue(userId);
    }

    public record TokenClaims(Long userId, Instant issuedAt) {
    }

    /** Devuelve el id de usuario si el token es válido, está firmado por nosotros y no ha expirado. */
    public Optional<Long> parseUserId(String token) {
        return parse(token).map(TokenClaims::userId);
    }

    public Optional<TokenClaims> parse(String token) {
        if (token == null || token.isBlank() || token.length() > 2048) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new TokenClaims(Long.parseLong(claims.getSubject()), claims.getIssuedAt().toInstant()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration ttl() {
        return ttl;
    }
}
