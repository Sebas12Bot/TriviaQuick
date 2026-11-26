package com.triviaquick.service;

import com.triviaquick.dto.ParticipantReportDTO;
import com.triviaquick.dto.ParticipantReportDTO.BonusDTO;
import com.triviaquick.dto.ParticipantReportDTO.QuestionReportDTO;
import com.triviaquick.exception.ApiException;
import com.triviaquick.mapper.RankingMapper;
import com.triviaquick.model.*;
import com.triviaquick.repository.GameResultRepository;
import com.triviaquick.repository.ParticipantRepository;
import com.triviaquick.repository.ScoreTransactionRepository;
import com.triviaquick.repository.SubmittedAnswerRepository;
import com.triviaquick.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reporte individual de un participante para el docente: qué respondió en cada pregunta,
 * si acertó, cuántos puntos ganó y qué bonificaciones recibió. Solo lo ven el docente anfitrión
 * y el propio participante; nunca otros estudiantes.
 */
@Service
public class ParticipantReportService {

    private static final Set<ScoreReason> BONUS_REASONS = EnumSet.of(ScoreReason.STREAK_BONUS, ScoreReason.FASTEST_ANSWER,
            ScoreReason.LIGHTNING_BONUS, ScoreReason.DUEL_WIN, ScoreReason.DUEL_LOSS, ScoreReason.SHIELD_BLOCK);

    private final SessionAccessService access;
    private final ParticipantRepository participants;
    private final SubmittedAnswerRepository answers;
    private final ScoreTransactionRepository transactions;
    private final GameResultRepository results;
    private final RankingService rankingService;

    public ParticipantReportService(SessionAccessService access, ParticipantRepository participants,
                                    SubmittedAnswerRepository answers, ScoreTransactionRepository transactions,
                                    GameResultRepository results, RankingService rankingService) {
        this.access = access;
        this.participants = participants;
        this.answers = answers;
        this.transactions = transactions;
        this.results = results;
        this.rankingService = rankingService;
    }

    @Transactional(readOnly = true)
    public ParticipantReportDTO report(Long sessionId, Long participantId, AuthUser viewer) {
        SessionAccessService.Viewer v = access.requireViewer(sessionId, viewer);
        SessionParticipant p = participants.findById(participantId)
                .filter(x -> x.getSession().getId().equals(sessionId))
                .orElseThrow(() -> ApiException.notFound("El participante no existe en esta sesión."));
        boolean self = v.participant() != null && v.participant().getId().equals(participantId);
        if (!v.host() && !self) {
            throw ApiException.forbidden("Solo el docente puede ver el reporte de otros participantes.");
        }
        GameSession session = v.session();

        Map<Long, SubmittedAnswer> byQuestion = answers.findWithDetailsByParticipant(participantId).stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));
        List<ScoreTransaction> txs = transactions.findByParticipantIdOrderByCreatedAtAscIdAsc(participantId);
        Set<Long> fastestQuestions = txs.stream().filter(t -> t.getReason() == ScoreReason.FASTEST_ANSWER && t.getQuestion() != null)
                .map(t -> t.getQuestion().getId()).collect(Collectors.toSet());

        // Preguntas ya planteadas: todas si terminó; si no, hasta la actual.
        int askedUpTo = session.isFinished() ? Integer.MAX_VALUE : session.getCurrentQuestionIndex();
        List<QuestionReportDTO> rows = new ArrayList<>();
        int correct = 0, wrong = 0, unanswered = 0;
        long timeSum = 0;
        for (Question q : session.getTrivia().getQuestions()) {
            SubmittedAnswer a = byQuestion.get(q.getId());
            String correctText = q.getAnswers().stream().filter(Answer::isCorrect).map(Answer::getText).findFirst().orElse(null);
            String outcome;
            if (a != null) {
                outcome = a.isCorrect() ? "ANSWERED_CORRECT" : "ANSWERED_WRONG";
                if (a.isCorrect()) correct++; else wrong++;
                timeSum += a.getResponseTimeMs();
            } else if (q.getPosition() <= askedUpTo && session.getStatus() != SessionStatus.LOBBY) {
                outcome = "UNANSWERED";
                unanswered++;
            } else {
                outcome = "NOT_ASKED";
            }
            rows.add(new QuestionReportDTO(q.getPosition(), q.getText(), q.getType(), outcome,
                    a == null ? null : a.getAnswer().getText(), "NOT_ASKED".equals(outcome) ? null : correctText,
                    a == null ? 0 : a.getPointsAwarded(), a == null ? 0 : a.getSpeedBonus(),
                    a == null ? 1 : a.getMultiplierFactor(), a == null ? null : a.getResponseTimeMs(),
                    fastestQuestions.contains(q.getId())));
        }
        int answered = correct + wrong;
        List<BonusDTO> bonuses = txs.stream().filter(t -> BONUS_REASONS.contains(t.getReason()))
                .map(t -> new BonusDTO(t.getReason(), t.getDelta(), t.getDescription())).toList();

        int position = session.isFinished()
                ? results.findOrderedBySession(sessionId).stream().filter(r -> r.getParticipant().getId().equals(participantId))
                .map(GameResult::getPosition).findFirst().orElse(0)
                : rankingService.provisional(sessionId).stream().filter(e -> e.input().participantId().equals(participantId))
                .map(RankingService.RankedEntry::position).findFirst().orElse(0);

        return new ParticipantReportDTO(p.getId(), p.getDisplayName(), RankingMapper.initials(p.getDisplayName()), position,
                p.getScore(), correct, wrong, unanswered, p.getBestStreak(), answered == 0 ? null : timeSum / answered, rows, bonuses);
    }
}
