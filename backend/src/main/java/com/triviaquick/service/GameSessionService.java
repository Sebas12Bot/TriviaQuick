package com.triviaquick.service;

import com.triviaquick.config.AppProperties;
import com.triviaquick.dto.SessionDtos.*;
import com.triviaquick.exception.ApiException;
import com.triviaquick.mapper.SessionMapper;
import com.triviaquick.model.*;
import com.triviaquick.repository.*;
import com.triviaquick.security.AuthUser;
import com.triviaquick.security.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ciclo de vida de una partida en vivo: LOBBY → QUESTION ⇄ EVENT → FINISHED.
 * El docente anfitrión controla el avance; las respuestas se validan y puntúan aquí.
 */
@Service
public class GameSessionService {

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final GameSessionRepository sessions;
    private final TriviaRepository trivias;
    private final QuestionRepository questions;
    private final ParticipantRepository participants;
    private final SubmittedAnswerRepository submittedAnswers;
    private final UserRepository users;
    private final ScoreService scoreService;
    private final GameEventService eventService;
    private final GameResultService resultService;
    private final RankingService rankingService;
    private final SessionAccessService access;
    private final SessionNotifier notifier;
    private final RateLimiter rateLimiter;
    private final AppProperties.Game rules;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public GameSessionService(GameSessionRepository sessions, TriviaRepository trivias, QuestionRepository questions,
                              ParticipantRepository participants, SubmittedAnswerRepository submittedAnswers,
                              UserRepository users, ScoreService scoreService, GameEventService eventService,
                              GameResultService resultService, RankingService rankingService,
                              SessionAccessService access, SessionNotifier notifier, RateLimiter rateLimiter,
                              AppProperties props, Clock clock) {
        this.sessions = sessions;
        this.trivias = trivias;
        this.questions = questions;
        this.participants = participants;
        this.submittedAnswers = submittedAnswers;
        this.users = users;
        this.scoreService = scoreService;
        this.eventService = eventService;
        this.resultService = resultService;
        this.rankingService = rankingService;
        this.access = access;
        this.notifier = notifier;
        this.rateLimiter = rateLimiter;
        this.rules = props.game();
        this.clock = clock;
    }

    // ------------------------------------------------------------------ creación y acceso

    @Transactional
    public SessionSummaryDTO create(CreateSessionDTO dto, AuthUser teacher) {
        Trivia trivia = trivias.findById(dto.triviaId())
                .orElseThrow(() -> ApiException.notFound("La trivia no existe."));
        if (!trivia.getOwner().getId().equals(teacher.id())) {
            throw ApiException.forbidden("Solo puedes lanzar partidas de tus propias trivias.");
        }
        if (!trivia.isPublished()) {
            throw ApiException.conflict("TRIVIA_DRAFT", "Publica el cuestionario antes de lanzar una partida.");
        }
        if (questions.countByTriviaId(trivia.getId()) == 0) {
            throw ApiException.badRequest("TRIVIA_EMPTY", "La trivia no tiene preguntas.");
        }
        // La configuración del cuestionario manda; la petición puede desactivar eventos o ajustar la frecuencia automática.
        EventMode mode = trivia.getEvents().getMode();
        boolean eventsEnabled = mode != EventMode.OFF && (dto.eventsEnabled() == null || dto.eventsEnabled());
        int frequency = dto.eventFrequency() == null ? trivia.getEvents().getAutoFrequency() : dto.eventFrequency();
        GameSession session = new GameSession(trivia, users.getReferenceById(teacher.id()), uniqueCode(),
                eventsEnabled, frequency, clock.instant());
        sessions.save(session);
        return SessionMapper.toSummary(session, true, 0);
    }

    @Transactional
    public SessionSummaryDTO join(JoinSessionDTO dto, AuthUser user) {
        if (user.role() != Role.STUDENT) {
            throw ApiException.forbidden("Solo los estudiantes pueden unirse a una partida.");
        }
        GameSession session = sessions.findByJoinCode(dto.code().toUpperCase())
                .orElseThrow(() -> ApiException.notFound("No hay ninguna partida con ese código."));
        if (session.isFinished()) {
            throw ApiException.conflict("SESSION_FINISHED", "Esa partida ya terminó.");
        }
        Optional<SessionParticipant> existing = participants.findBySessionIdAndUserId(session.getId(), user.id());
        if (existing.isEmpty()) {
            participants.saveAndFlush(new SessionParticipant(session, users.getReferenceById(user.id()), clock.instant()));
            notifier.notify(session.getId(), SessionNotifier.Type.PARTICIPANT_JOINED);
        }
        return SessionMapper.toSummary(session, false, participants.countBySessionId(session.getId()));
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryDTO> listMine(AuthUser user) {
        return sessions.findVisibleTo(user.id()).stream()
                .map(s -> SessionMapper.toSummary(s, s.isHostedBy(user.id()), participants.countBySessionId(s.getId())))
                .toList();
    }

    // ------------------------------------------------------------------ control del anfitrión

    @Transactional
    public SessionStateDTO start(Long sessionId, AuthUser host) {
        GameSession session = lockForHost(sessionId, host);
        if (session.getStatus() != SessionStatus.LOBBY) {
            throw ApiException.conflict("SESSION_ALREADY_STARTED", "La partida ya comenzó.");
        }
        if (participants.countBySessionId(sessionId) == 0) {
            throw ApiException.conflict("NO_PARTICIPANTS", "Espera a que al menos un participante se una.");
        }
        startQuestion(session, 0);
        return stateFor(sessionId, host);
    }

    /**
     * Avanza la partida: de una pregunta pasa al minijuego programado o a la siguiente pregunta;
     * de un minijuego, a la siguiente pregunta. Tras la última pregunta, cierra la partida.
     */
    @Transactional
    public SessionStateDTO advance(Long sessionId, AuthUser host) {
        GameSession session = lockForHost(sessionId, host);
        int total = (int) questions.countByTriviaId(session.getTrivia().getId());
        switch (session.getStatus()) {
            case LOBBY -> throw ApiException.conflict("SESSION_NOT_STARTED", "Primero inicia la partida.");
            case FINISHED -> throw ApiException.conflict("SESSION_FINISHED", "La partida ya terminó.");
            case QUESTION -> {
                int index = session.getCurrentQuestionIndex();
                awardFastestAnswer(session);
                Optional<GameEvent> event = eventService.maybeStart(session, index, total);
                if (event.isPresent()) {
                    session.enterEvent();
                } else if (index >= total - 1) {
                    finalizeSession(session);
                } else {
                    startQuestion(session, index + 1);
                }
            }
            case EVENT -> {
                eventService.resolveActive(session);
                int next = session.getCurrentQuestionIndex() + 1;
                if (next >= total) {
                    finalizeSession(session);
                } else {
                    startQuestion(session, next);
                }
            }
        }
        return stateFor(sessionId, host);
    }

    /** Cierre anticipado por el anfitrión. */
    @Transactional
    public SessionStateDTO finish(Long sessionId, AuthUser host) {
        GameSession session = lockForHost(sessionId, host);
        if (session.isFinished()) {
            throw ApiException.conflict("SESSION_FINISHED", "La partida ya terminó.");
        }
        if (session.getStatus() == SessionStatus.EVENT) {
            eventService.resolveActive(session);
        }
        if (session.getStatus() == SessionStatus.QUESTION) {
            awardFastestAnswer(session);
        }
        finalizeSession(session);
        return stateFor(sessionId, host);
    }

    /**
     * Al cerrar una pregunta, premia a quien la acertó en menos tiempo (tiempo medido por el servidor;
     * a igual tiempo gana quien llegó antes). Se ejecuta con la sesión bloqueada en exclusiva, así que
     * ninguna respuesta puede entrar mientras tanto, y su clave de idempotencia impide repetirlo.
     */
    private void awardFastestAnswer(GameSession lockedSession) {
        Trivia trivia = lockedSession.getTrivia();
        if (!trivia.getScoring().isFastestBonusEnabled()) {
            return;
        }
        Question question = questions.findWithAnswersByTriviaAndPosition(trivia.getId(),
                lockedSession.getCurrentQuestionIndex()).orElseThrow();
        String key = "FASTEST:" + lockedSession.getId() + ":" + question.getId();
        if (scoreService.alreadyApplied(key)) {
            return;
        }
        submittedAnswers.findCorrectFastestFirst(question.getId(), lockedSession.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst()
                .ifPresent(fastest -> {
                    SessionParticipant winner = participants.findByIdForUpdate(fastest.getParticipant().getId()).orElseThrow();
                    scoreService.award(winner, trivia.getScoring().getFastestBonusPoints(), ScoreReason.FASTEST_ANSWER,
                            question, null, "Respuesta correcta más rápida de la pregunta " + (question.getPosition() + 1)
                                    + String.format(" (%.1f s)", fastest.getResponseTimeMs() / 1000.0), key);
                    notifier.notify(lockedSession.getId(), SessionNotifier.Type.RANKING_UPDATED, null,
                            Map.of("fastestParticipantId", winner.getId()));
                });
    }

    private GameSession lockForHost(Long sessionId, AuthUser host) {
        GameSession session = sessions.findByIdForUpdate(sessionId)
                .orElseThrow(() -> ApiException.notFound("La sesión no existe."));
        access.requireHost(session, host);
        return session;
    }

    private void startQuestion(GameSession session, int index) {
        Question q = questions.findWithAnswersByTriviaAndPosition(session.getTrivia().getId(), index).orElseThrow();
        session.startQuestion(index, clock.instant(), q.getTimeLimitSeconds());
        notifier.notify(session.getId(), SessionNotifier.Type.QUESTION_STARTED, session.getQuestionDeadline(),
                Map.of("index", index));
    }

    private void finalizeSession(GameSession session) {
        session.finish(clock.instant());
        sessions.flush();
        resultService.persistFinalResults(session);
        notifier.notify(session.getId(), SessionNotifier.Type.SESSION_FINISHED);
    }

    // ------------------------------------------------------------------ respuestas

    /**
     * Registra una respuesta. El cliente solo indica pregunta y opción; el servidor decide si es correcta,
     * cuántos puntos vale (según la rapidez) y si aplica un multiplicador activo.
     */
    @Transactional
    public AnswerResultDTO submitAnswer(Long sessionId, AuthUser user, SubmitAnswerDTO dto) {
        rateLimiter.check(user.id());
        // Bloqueo compartido: el anfitrión no puede avanzar ni cerrar mientras esta respuesta se procesa.
        GameSession session = sessions.findByIdForShare(sessionId)
                .orElseThrow(() -> ApiException.notFound("La sesión no existe."));
        SessionParticipant me = access.requireParticipant(sessionId, user);
        if (session.isFinished()) {
            throw ApiException.conflict("SESSION_FINISHED", "La partida ya terminó y no admite respuestas.");
        }
        if (session.getStatus() != SessionStatus.QUESTION) {
            throw ApiException.conflict("NO_ACTIVE_QUESTION", "No hay ninguna pregunta abierta ahora mismo.");
        }
        Question question = questions.findWithAnswersByTriviaAndPosition(session.getTrivia().getId(),
                session.getCurrentQuestionIndex()).orElseThrow();
        if (!question.getId().equals(dto.questionId())) {
            throw ApiException.conflict("QUESTION_NOT_ACTIVE", "Esa pregunta no es la que está en curso.");
        }
        Instant now = clock.instant();
        if (now.isAfter(session.getQuestionDeadline().plusMillis(rules.answerGraceMillis()))) {
            throw ApiException.conflict("TIME_EXPIRED", "Se acabó el tiempo para esta pregunta.");
        }
        Answer answer = question.findAnswer(dto.answerId())
                .orElseThrow(() -> ApiException.badRequest("ANSWER_NOT_IN_QUESTION", "La opción no pertenece a esta pregunta."));

        SessionParticipant locked = participants.findByIdForUpdate(me.getId()).orElseThrow();
        if (submittedAnswers.existsByParticipantIdAndQuestionId(locked.getId(), question.getId())) {
            throw ApiException.conflict("ANSWER_ALREADY_SUBMITTED", "Ya respondiste esta pregunta.");
        }
        // El tiempo se mide con el reloj del servidor desde el inicio de la pregunta; el cliente no lo declara.
        long responseMillis = Math.max(0, Duration.between(session.getQuestionStartedAt(), now).toMillis());
        boolean correct = answer.isCorrect();
        Trivia trivia = session.getTrivia();
        int factor = scoreService.consumeActiveMultiplier(locked.getId());
        ScoreService.AnswerScore score = correct
                ? new ScoreService.AnswerScore(question.getBasePoints(),
                trivia.speedBonusFor(ScoreService.elapsedPercent(question.getTimeLimitSeconds(), responseMillis)), factor)
                : new ScoreService.AnswerScore(0, 0, 1);
        int points = score.total();
        boolean multiplied = correct && factor > 1;
        SubmittedAnswer entity = new SubmittedAnswer(locked, question, answer, correct, points, multiplied,
                (int) Math.min(responseMillis, Integer.MAX_VALUE), now);
        entity.recordBreakdown(score.basePoints(), score.speedBonus(), correct ? factor : 1);
        SubmittedAnswer submitted = submittedAnswers.saveAndFlush(entity);
        locked.registerAnswer(correct);
        if (points > 0) {
            scoreService.award(locked, points, ScoreReason.CORRECT_ANSWER, question, null,
                    "Respuesta correcta a la pregunta " + (question.getPosition() + 1) + (multiplied ? " (x" + factor + ")" : ""),
                    "ANSWER:" + submitted.getId());
        }
        int streak = locked.updateStreak(correct, question.getPosition(), trivia.getScoring().isStreakBreaksOnMiss());
        int streakBonus = correct ? trivia.streakBonusFor(streak) : 0;
        if (streakBonus > 0) {
            scoreService.award(locked, streakBonus, ScoreReason.STREAK_BONUS, question, null,
                    "Racha de " + streak + " aciertos seguidos", "STREAK:" + submitted.getId());
        }
        notifier.notify(sessionId, SessionNotifier.Type.ANSWER_RECEIVED, session.getQuestionDeadline(),
                Map.of("answers", submittedAnswers.countByQuestionIdAndParticipantSessionId(question.getId(), sessionId)));
        Long correctId = question.getAnswers().stream().filter(Answer::isCorrect).map(Answer::getId).findFirst().orElse(null);
        return new AnswerResultDTO(correct, points, multiplied, locked.getScore(), correctId,
                score.basePoints(), score.speedBonus(), multiplied ? factor : 1, streak, streakBonus);
    }

    // ------------------------------------------------------------------ estado

    @Transactional(readOnly = true)
    public SessionStateDTO stateFor(Long sessionId, AuthUser user) {
        SessionAccessService.Viewer viewer = access.requireViewer(sessionId, user);
        GameSession session = viewer.session();
        int total = (int) questions.countByTriviaId(session.getTrivia().getId());

        QuestionViewDTO current = null;
        MyAnswerDTO myAnswer = null;
        Long answersReceived = null;
        if (session.getStatus() == SessionStatus.QUESTION) {
            Question q = questions.findWithAnswersByTriviaAndPosition(session.getTrivia().getId(),
                    session.getCurrentQuestionIndex()).orElseThrow();
            current = SessionMapper.toQuestionView(q, total, session);
            if (viewer.participant() != null) {
                myAnswer = submittedAnswers.findByParticipantIdAndQuestionId(viewer.participant().getId(), q.getId())
                        .map(SessionMapper::toMyAnswer).orElse(null);
            }
            if (viewer.host()) {
                answersReceived = submittedAnswers.countByQuestionIdAndParticipantSessionId(q.getId(), sessionId);
            }
        }

        MyStandingDTO me = null;
        List<com.triviaquick.dto.PowerUpDTO> myPowerUps = List.of();
        if (viewer.participant() != null) {
            SessionParticipant p = viewer.participant();
            int position = rankingService.provisional(sessionId).stream()
                    .filter(e -> e.input().participantId().equals(p.getId()))
                    .map(RankingService.RankedEntry::position).findFirst().orElse(0);
            me = new MyStandingDTO(p.getId(), p.getDisplayName(), p.getScore(), p.getCorrectAnswers(),
                    p.getAnsweredCount(), position, p.getCurrentStreak(), p.getBestStreak());
            myPowerUps = eventService.powerUpsOf(p.getId());
        }
        List<ParticipantBriefDTO> roster = participants.findBySessionIdOrderByJoinedAtAsc(sessionId).stream()
                .map(SessionMapper::toBrief).toList();

        return new SessionStateDTO(session.getId(), session.getTrivia().getTitle(), session.getStatus(),
                viewer.host() ? ViewerRole.HOST : ViewerRole.PARTICIPANT,
                viewer.host() ? session.getJoinCode() : null,
                clock.instant(), total, current, myAnswer, answersReceived,
                eventService.currentFor(viewer).orElse(null), me, myPowerUps, roster,
                session.getCurrentQuestionIndex() >= total - 1);
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            char[] code = new char[6];
            for (int i = 0; i < code.length; i++) {
                code[i] = CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)];
            }
            String candidate = new String(code);
            if (!sessions.existsByJoinCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("No se pudo generar un código de acceso único");
    }
}
