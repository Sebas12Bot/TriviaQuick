package com.triviaquick.security;

import com.triviaquick.config.AppProperties;
import com.triviaquick.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de ventana deslizante por usuario para los endpoints de respuesta.
 * Es en memoria (una instancia); con varias réplicas debería moverse a un almacén compartido.
 */
@Component
public class RateLimiter {

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final Map<Long, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimiter(AppProperties props, Clock clock) {
        this.limit = props.rateLimit().answersPerWindow();
        this.windowMillis = props.rateLimit().windowSeconds() * 1000L;
        this.clock = clock;
    }

    public void check(Long userId) {
        long now = clock.millis();
        Deque<Long> deque = hits.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > windowMillis) {
                deque.pollFirst();
            }
            if (deque.size() >= limit) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                        "Demasiadas respuestas seguidas. Espera unos segundos.");
            }
            deque.addLast(now);
        }
    }

    public void reset() {
        hits.clear();
    }
}
