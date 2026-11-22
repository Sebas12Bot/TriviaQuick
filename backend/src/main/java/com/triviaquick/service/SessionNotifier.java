package com.triviaquick.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;

/**
 * Publica los mensajes oficiales del servidor en {@code /topic/sessions/{id}}.
 * Se envían solo después de confirmar la transacción, para que ningún cliente vea un estado
 * que luego se revierta. Los mensajes no incluyen respuestas correctas ni datos privados:
 * indican qué cambió y cada cliente consulta su proyección autorizada por REST.
 */
@Service
public class SessionNotifier {

    public enum Type {
        PARTICIPANT_JOINED, QUESTION_STARTED, ANSWER_RECEIVED, EVENT_STARTED, EVENT_UPDATED, EVENT_RESOLVED,
        RANKING_UPDATED, SESSION_FINISHED
    }

    public record SessionMessage(Long sessionId, Type type, Instant serverTime, Instant deadline,
                                 Map<String, Object> data) {
    }

    private final ApplicationEventPublisher publisher;
    private final SimpMessagingTemplate messaging;
    private final Clock clock;

    public SessionNotifier(ApplicationEventPublisher publisher, SimpMessagingTemplate messaging, Clock clock) {
        this.publisher = publisher;
        this.messaging = messaging;
        this.clock = clock;
    }

    public void notify(Long sessionId, Type type, Instant deadline, Map<String, Object> data) {
        publisher.publishEvent(new SessionMessage(sessionId, type, clock.instant(), deadline, data));
    }

    public void notify(Long sessionId, Type type) {
        notify(sessionId, type, null, Map.of());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void send(SessionMessage message) {
        messaging.convertAndSend("/topic/sessions/" + message.sessionId(), message);
    }
}
