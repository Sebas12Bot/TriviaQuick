package com.triviaquick.service;

import com.triviaquick.exception.ApiException;
import com.triviaquick.model.GameSession;
import com.triviaquick.model.SessionParticipant;
import com.triviaquick.repository.GameSessionRepository;
import com.triviaquick.repository.ParticipantRepository;
import com.triviaquick.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Autorización a nivel de sesión. Una sesión solo es visible para su anfitrión (el docente que la creó)
 * y para sus participantes. La identidad siempre llega del contexto de autenticación.
 */
@Service
public class SessionAccessService {

    private final GameSessionRepository sessions;
    private final ParticipantRepository participants;

    public SessionAccessService(GameSessionRepository sessions, ParticipantRepository participants) {
        this.sessions = sessions;
        this.participants = participants;
    }

    public record Viewer(GameSession session, SessionParticipant participant, boolean host) {
    }

    @Transactional(readOnly = true)
    public Viewer requireViewer(Long sessionId, AuthUser user) {
        GameSession session = sessions.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("La sesión no existe."));
        if (session.isHostedBy(user.id())) {
            return new Viewer(session, null, true);
        }
        SessionParticipant participant = participants.findBySessionIdAndUserId(sessionId, user.id())
                .orElseThrow(() -> ApiException.forbidden("No participas en esta sesión."));
        return new Viewer(session, participant, false);
    }

    public void requireHost(GameSession session, AuthUser user) {
        if (!session.isHostedBy(user.id())) {
            throw ApiException.forbidden("Solo el docente que creó la sesión puede controlarla.");
        }
    }

    public SessionParticipant requireParticipant(Long sessionId, AuthUser user) {
        return participants.findBySessionIdAndUserId(sessionId, user.id())
                .orElseThrow(() -> ApiException.forbidden("No participas en esta sesión."));
    }

    /** Usado por el canal WebSocket para autorizar suscripciones. */
    @Transactional(readOnly = true)
    public boolean canView(Long sessionId, Long userId) {
        Optional<GameSession> session = sessions.findById(sessionId);
        return session.isPresent()
                && (session.get().isHostedBy(userId) || participants.existsBySessionIdAndUserId(sessionId, userId));
    }
}
