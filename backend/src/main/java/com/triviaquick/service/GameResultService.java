package com.triviaquick.service;

import com.triviaquick.dto.GameResultDTO;
import com.triviaquick.dto.ParticipantRankingDTO;
import com.triviaquick.dto.PodiumDTO;
import com.triviaquick.dto.ScoreTransactionDTO;
import com.triviaquick.exception.ApiException;
import com.triviaquick.mapper.ScoreTransactionMapper;
import com.triviaquick.model.GameResult;
import com.triviaquick.model.GameSession;
import com.triviaquick.model.SessionParticipant;
import com.triviaquick.repository.GameResultRepository;
import com.triviaquick.repository.ParticipantRepository;
import com.triviaquick.repository.QuestionRepository;
import com.triviaquick.repository.ScoreTransactionRepository;
import com.triviaquick.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resultados definitivos: se calculan una sola vez al cerrar la sesión y después son inmutables. */
@Service
public class GameResultService {

    private final GameResultRepository results;
    private final ParticipantRepository participants;
    private final QuestionRepository questions;
    private final ScoreTransactionRepository transactions;
    private final RankingService rankingService;
    private final SessionAccessService access;
    private final Clock clock;

    public GameResultService(GameResultRepository results, ParticipantRepository participants,
                             QuestionRepository questions, ScoreTransactionRepository transactions,
                             RankingService rankingService, SessionAccessService access, Clock clock) {
        this.results = results;
        this.participants = participants;
        this.questions = questions;
        this.transactions = transactions;
        this.rankingService = rankingService;
        this.access = access;
        this.clock = clock;
    }

    /** Persiste la clasificación definitiva. Idempotente: si ya existe no hace nada. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void persistFinalResults(GameSession lockedSession) {
        if (!lockedSession.isFinished()) {
            throw new IllegalStateException("Solo se generan resultados de sesiones finalizadas");
        }
        if (results.existsBySessionId(lockedSession.getId())) {
            return;
        }
        List<SessionParticipant> roster = participants.findBySessionIdOrderByJoinedAtAsc(lockedSession.getId());
        Map<Long, SessionParticipant> byId = roster.stream()
                .collect(Collectors.toMap(SessionParticipant::getId, Function.identity()));
        Instant now = clock.instant();
        for (RankingService.RankedEntry entry : RankingService.rank(roster.stream().map(RankingService::toInput).toList())) {
            SessionParticipant p = byId.get(entry.input().participantId());
            results.save(new GameResult(lockedSession, p, entry.position(), p.getScore(), p.getCorrectAnswers(),
                    p.getAnsweredCount(), now));
        }
    }

    @Transactional(readOnly = true)
    public GameResultDTO resultsFor(Long sessionId, AuthUser viewer) {
        GameSession session = requireFinished(sessionId, viewer);
        List<ParticipantRankingDTO> ranking = rankingService.finalEntries(sessionId, viewer.id());
        ParticipantRankingDTO mine = ranking.stream().filter(ParticipantRankingDTO::currentUser).findFirst().orElse(null);
        return new GameResultDTO(sessionId, session.getTrivia().getTitle(), session.getFinishedAt(), ranking.size(),
                (int) questions.countByTriviaId(session.getTrivia().getId()), mine,
                RankingService.podium(sessionId, ranking), ranking, RankingService.TIE_BREAK_RULE);
    }

    @Transactional(readOnly = true)
    public PodiumDTO podiumFor(Long sessionId, AuthUser viewer) {
        requireFinished(sessionId, viewer);
        return RankingService.podium(sessionId, rankingService.finalEntries(sessionId, viewer.id()));
    }

    /** Historial de puntos: el anfitrión ve todos los movimientos; un participante solo los suyos. */
    @Transactional(readOnly = true)
    public List<ScoreTransactionDTO> transactionsFor(Long sessionId, AuthUser viewer) {
        SessionAccessService.Viewer v = access.requireViewer(sessionId, viewer);
        var rows = v.host()
                ? transactions.findBySessionIdOrderByCreatedAtAscIdAsc(sessionId)
                : transactions.findByParticipantIdOrderByCreatedAtAscIdAsc(v.participant().getId());
        return rows.stream().map(ScoreTransactionMapper::toDto).toList();
    }

    private GameSession requireFinished(Long sessionId, AuthUser viewer) {
        GameSession session = access.requireViewer(sessionId, viewer).session();
        if (!session.isFinished()) {
            throw ApiException.conflict("RESULTS_NOT_READY", "Los resultados estarán disponibles al terminar la partida.");
        }
        return session;
    }
}
