package com.triviaquick.service;

import com.triviaquick.dto.TriviaDtos.*;
import com.triviaquick.exception.ApiException;
import com.triviaquick.mapper.TriviaMapper;
import com.triviaquick.model.*;
import com.triviaquick.repository.GameSessionRepository;
import com.triviaquick.repository.TriviaRepository;
import com.triviaquick.repository.UserRepository;
import com.triviaquick.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * Administración de cuestionarios del docente: borradores, publicación, preguntas,
 * reglas de puntuación y línea de tiempo de eventos.
 *
 * <p>Reglas de edición:
 * <ul>
 *   <li>Solo el propietario puede ver o modificar un cuestionario.</li>
 *   <li>Con una partida en curso (no finalizada) no se puede modificar nada.</li>
 *   <li>Si ya se jugó alguna partida, las preguntas y respuestas quedan bloqueadas para no alterar
 *   el historial; se puede duplicar el cuestionario para editarlo.</li>
 *   <li>Un borrador puede estar incompleto; publicar exige un cuestionario válido y jugable.</li>
 * </ul>
 */
@Service
public class TriviaService {

    private static final List<SpeedTierDTO> DEFAULT_SPEED = List.of(
            new SpeedTierDTO(25, 100), new SpeedTierDTO(60, 50), new SpeedTierDTO(100, 10));
    private static final List<StreakTierDTO> DEFAULT_STREAK = List.of(
            new StreakTierDTO(3, 30), new StreakTierDTO(5, 60), new StreakTierDTO(10, 100));

    private final TriviaRepository trivias;
    private final UserRepository users;
    private final GameSessionRepository sessions;
    private final Clock clock;

    public TriviaService(TriviaRepository trivias, UserRepository users, GameSessionRepository sessions, Clock clock) {
        this.trivias = trivias;
        this.users = users;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public TriviaDetailDTO create(CreateTriviaDTO dto, AuthUser teacher) {
        Instant now = clock.instant();
        Trivia trivia = new Trivia(dto.title().trim(), blankToNull(dto.description()),
                users.getReferenceById(teacher.id()), now);
        apply(trivia, dto, true, now);
        return TriviaMapper.toDetail(trivias.save(trivia), false, false);
    }

    @Transactional
    public TriviaDetailDTO update(Long id, CreateTriviaDTO dto, AuthUser teacher) {
        Trivia trivia = owned(id, teacher);
        if (sessions.existsByTriviaIdAndStatusNot(id, SessionStatus.FINISHED)) {
            throw ApiException.conflict("TRIVIA_IN_USE", "Hay una partida en curso con este cuestionario. Termínala antes de editarlo.");
        }
        boolean locked = sessions.countByTriviaId(id) > 0;
        apply(trivia, dto, !locked, clock.instant());
        trivias.flush();
        return TriviaMapper.toDetail(trivia, locked, false);
    }

    @Transactional
    public TriviaDetailDTO duplicate(Long id, AuthUser teacher) {
        Trivia source = owned(id, teacher);
        Instant now = clock.instant();
        Trivia copy = new Trivia(truncate(source.getTitle() + " (copia)", 140), source.getDescription(),
                users.getReferenceById(teacher.id()), now);
        CreateTriviaDTO asDto = toDto(source);
        apply(copy, new CreateTriviaDTO(copy.getTitle(), asDto.description(), asDto.category(), TriviaStatus.DRAFT,
                asDto.playMode(), asDto.coverImageKey(), asDto.activityMinutes(), asDto.questions(), asDto.scoring(),
                asDto.events(), asDto.timeline()), true, now);
        return TriviaMapper.toDetail(trivias.save(copy), false, false);
    }

    @Transactional
    public void delete(Long id, AuthUser teacher) {
        Trivia trivia = owned(id, teacher);
        if (sessions.countByTriviaId(id) > 0) {
            throw ApiException.conflict("TRIVIA_HAS_SESSIONS", "Este cuestionario ya se jugó y sus resultados se conservan; no se puede eliminar.");
        }
        trivias.delete(trivia);
    }

    @Transactional(readOnly = true)
    public List<TriviaSummaryDTO> listMine(AuthUser teacher) {
        return trivias.findByOwnerIdOrderByCreatedAtDesc(teacher.id()).stream()
                .map(t -> TriviaMapper.toSummary(t, sessions.countByTriviaId(t.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public TriviaDetailDTO get(Long id, AuthUser teacher) {
        Trivia trivia = owned(id, teacher);
        return TriviaMapper.toDetail(trivia, sessions.countByTriviaId(id) > 0,
                sessions.existsByTriviaIdAndStatusNot(id, SessionStatus.FINISHED));
    }

    private Trivia owned(Long id, AuthUser teacher) {
        Trivia trivia = trivias.findById(id).orElseThrow(() -> ApiException.notFound("El cuestionario no existe."));
        if (!trivia.getOwner().getId().equals(teacher.id())) {
            throw ApiException.forbidden("Este cuestionario pertenece a otro docente.");
        }
        return trivia;
    }

    // ------------------------------------------------------------------ aplicación y validación

    private void apply(Trivia trivia, CreateTriviaDTO dto, boolean replaceQuestions, Instant now) {
        TriviaStatus status = dto.status() == null ? TriviaStatus.PUBLISHED : dto.status();
        ScoringDTO sc = dto.scoring();
        int defaultPoints = sc != null && sc.defaultPoints() != null ? sc.defaultPoints() : 1000;
        ScoringConfig scoring = new ScoringConfig(defaultPoints,
                sc == null || sc.speedBonusEnabled() == null || sc.speedBonusEnabled(),
                sc == null || sc.streakEnabled() == null || sc.streakEnabled(),
                sc == null || sc.streakBreaksOnMiss() == null || sc.streakBreaksOnMiss(),
                sc == null || sc.fastestBonusEnabled() == null || sc.fastestBonusEnabled(),
                sc != null && sc.fastestBonusPoints() != null ? sc.fastestBonusPoints() : 100);

        List<CreateQuestionDTO> questions = dto.questions();
        if (!replaceQuestions && !sameQuestions(trivia, questions)) {
            throw ApiException.conflict("QUESTIONS_LOCKED",
                    "Este cuestionario ya se jugó: sus preguntas no se pueden modificar. Duplícalo para crear una versión editable.");
        }
        validateQuestions(questions, status == TriviaStatus.PUBLISHED);
        List<TimelineEventDTO> timeline = dto.timeline() == null ? List.of() : dto.timeline();
        EventSettingsDTO ev = dto.events();
        EventMode mode = ev != null && ev.mode() != null ? ev.mode() : EventMode.AUTO;
        Set<GameEventType> autoTypes = ev != null && ev.autoTypes() != null ? ev.autoTypes() : EnumSet.allOf(GameEventType.class);
        if (mode == EventMode.AUTO && autoTypes.isEmpty()) {
            throw ApiException.badRequest("INVALID_EVENTS", "En modo automático habilita al menos un tipo de minijuego.");
        }
        validateTimeline(mode, timeline, questions.size());

        trivia.updateDetails(dto.title().trim(), blankToNull(dto.description()), blankToNull(dto.category()), status,
                dto.playMode() == null ? PlayMode.GROUP : dto.playMode(), dto.coverImageKey(), dto.activityMinutes(),
                scoring, now);

        AbilityConfigDTO ab = ev != null && ev.abilities() != null ? ev.abilities()
                : new AbilityConfigDTO(null, null, null, null, null, null, null, null, null);
        trivia.getEvents().update(mode,
                ev != null && ev.autoFrequency() != null ? ev.autoFrequency() : 2,
                autoTypes.isEmpty() ? EnumSet.noneOf(GameEventType.class) : EnumSet.copyOf(autoTypes),
                or(ab.lightningSeconds(), 20), or(ab.lightningReward(), 300), or(ab.lightningCategory(), ChallengeCategory.ANY),
                or(ab.multiplierFactor(), 2), or(ab.multiplierTarget(), PowerUpTarget.BOTTOM_HALF),
                or(ab.shieldTarget(), PowerUpTarget.CORRECT_PREVIOUS), or(ab.duelSeconds(), 20), or(ab.duelStake(), 250),
                or(ab.powerUpSeconds(), 15));
        if (ab.multiplierTarget() == PowerUpTarget.CORRECT_PREVIOUS) {
            throw ApiException.badRequest("INVALID_EVENTS", "El multiplicador se reparte a la mitad inferior o a todos.");
        }
        if (ab.shieldTarget() == PowerUpTarget.BOTTOM_HALF) {
            throw ApiException.badRequest("INVALID_EVENTS", "El escudo se reparte a quienes acertaron la pregunta anterior o a todos.");
        }

        replaceTiers(trivia, sc);

        if (replaceQuestions) {
            // Se vacían y se recrean: el índice único (trivia, posición) exige borrar antes de insertar.
            trivia.getQuestions().clear();
            trivias.flush();
            for (CreateQuestionDTO q : questions) {
                QuestionType type = q.type() == null ? QuestionType.MULTIPLE_CHOICE : q.type();
                Question question = new Question(q.text().trim(), q.timeLimitSeconds(), q.basePoints(), type, q.imageKey());
                question.setVideoKey(q.videoKey());
                q.answers().forEach(a -> question.addAnswer(new Answer(a.text().trim(), a.correct())));
                trivia.addQuestion(question);
            }
        }

        trivia.getTimeline().clear();
        trivias.flush();
        if (mode == EventMode.MANUAL) {
            for (TimelineEventDTO e : timeline) {
                trivia.getTimeline().add(new TimelineEvent(trivia, e.afterQuestion(), e.type(), e.durationSeconds(),
                        e.rewardPoints(), e.type() == GameEventType.MULTIPLIER ? e.multiplierFactor() : null,
                        e.type() == GameEventType.DUEL ? e.stake() : null,
                        e.type() == GameEventType.LIGHTNING_CHALLENGE ? e.challengeCategory() : null,
                        validTarget(e)));
            }
        }
    }

    private void replaceTiers(Trivia trivia, ScoringDTO sc) {
        List<SpeedTierDTO> speed = sc != null && sc.speedTiers() != null ? sc.speedTiers() : DEFAULT_SPEED;
        List<StreakTierDTO> streak = sc != null && sc.streakTiers() != null ? sc.streakTiers() : DEFAULT_STREAK;
        if (trivia.getScoring().isSpeedBonusEnabled() && speed.isEmpty()) {
            throw ApiException.badRequest("INVALID_SCORING", "Añade al menos un tramo de bonificación por velocidad o desactívala.");
        }
        if (trivia.getScoring().isStreakEnabled() && streak.isEmpty()) {
            throw ApiException.badRequest("INVALID_SCORING", "Añade al menos un tramo de racha o desactiva las rachas.");
        }
        if (speed.stream().map(SpeedTierDTO::maxElapsedPercent).distinct().count() != speed.size()) {
            throw ApiException.badRequest("INVALID_SCORING", "Los tramos de velocidad no pueden repetir el mismo porcentaje.");
        }
        List<SpeedTierDTO> sortedSpeed = speed.stream().sorted(Comparator.comparingInt(SpeedTierDTO::maxElapsedPercent)).toList();
        for (int i = 1; i < sortedSpeed.size(); i++) {
            if (sortedSpeed.get(i).bonusPoints() > sortedSpeed.get(i - 1).bonusPoints()) {
                throw ApiException.badRequest("INVALID_SCORING", "La bonificación por velocidad debe disminuir conforme pasa el tiempo.");
            }
        }
        if (streak.stream().map(StreakTierDTO::streakLength).distinct().count() != streak.size()) {
            throw ApiException.badRequest("INVALID_SCORING", "Los tramos de racha no pueden repetir la misma longitud.");
        }
        trivia.getSpeedTiers().clear();
        trivia.getStreakTiers().clear();
        trivias.flush();
        sortedSpeed.forEach(t -> trivia.getSpeedTiers().add(new SpeedBonusTier(trivia, t.maxElapsedPercent(), t.bonusPoints())));
        streak.stream().sorted(Comparator.comparingInt(StreakTierDTO::streakLength))
                .forEach(t -> trivia.getStreakTiers().add(new StreakBonusTier(trivia, t.streakLength(), t.bonusPoints())));
    }

    private static PowerUpTarget validTarget(TimelineEventDTO e) {
        if (e.target() == null) {
            return null;
        }
        boolean ok = switch (e.type()) {
            case MULTIPLIER -> e.target() != PowerUpTarget.CORRECT_PREVIOUS;
            case SHIELD -> e.target() != PowerUpTarget.BOTTOM_HALF;
            default -> false;
        };
        if (!ok) {
            throw ApiException.badRequest("INVALID_EVENTS", "El destinatario elegido no es válido para ese minijuego.");
        }
        return e.target();
    }

    private static void validateQuestions(List<CreateQuestionDTO> questions, boolean publishing) {
        if (publishing && questions.isEmpty()) {
            throw ApiException.badRequest("INVALID_QUESTION", "Para publicar, el cuestionario necesita al menos una pregunta.");
        }
        for (int i = 0; i < questions.size(); i++) {
            CreateQuestionDTO q = questions.get(i);
            long correct = q.answers().stream().filter(CreateAnswerDTO::correct).count();
            if (publishing && correct != 1) {
                throw ApiException.badRequest("INVALID_QUESTION",
                        "La pregunta " + (i + 1) + " debe tener exactamente una respuesta correcta.");
            }
            if (correct > 1) {
                throw ApiException.badRequest("INVALID_QUESTION", "La pregunta " + (i + 1) + " tiene más de una respuesta correcta.");
            }
            if (q.type() == QuestionType.TRUE_FALSE && q.answers().size() != 2) {
                throw ApiException.badRequest("INVALID_QUESTION", "La pregunta " + (i + 1) + " es de verdadero/falso y debe tener 2 opciones.");
            }
        }
    }

    /** Eventos manuales: solo entre dos preguntas y como máximo uno por hueco. */
    private static void validateTimeline(EventMode mode, List<TimelineEventDTO> timeline, int questionCount) {
        if (mode != EventMode.MANUAL) {
            return;
        }
        Set<Integer> slots = new HashSet<>();
        for (TimelineEventDTO e : timeline) {
            if (e.afterQuestion() >= questionCount - 1) {
                throw ApiException.badRequest("INVALID_TIMELINE", "Los minijuegos deben ir entre dos preguntas, no al final.");
            }
            if (!slots.add(e.afterQuestion())) {
                throw ApiException.badRequest("INVALID_TIMELINE", "Solo puede haber un minijuego entre dos preguntas.");
            }
        }
    }

    /** Con preguntas bloqueadas, el documento debe traer exactamente las mismas preguntas y respuestas. */
    private static boolean sameQuestions(Trivia trivia, List<CreateQuestionDTO> incoming) {
        List<Question> current = trivia.getQuestions();
        if (current.size() != incoming.size()) {
            return false;
        }
        for (int i = 0; i < current.size(); i++) {
            Question q = current.get(i);
            CreateQuestionDTO d = incoming.get(i);
            QuestionType type = d.type() == null ? QuestionType.MULTIPLE_CHOICE : d.type();
            if (!q.getText().equals(d.text().trim()) || q.getTimeLimitSeconds() != d.timeLimitSeconds()
                    || q.getBasePoints() != d.basePoints() || q.getType() != type
                    || !Objects.equals(q.getImageKey(), d.imageKey()) || !Objects.equals(q.getVideoKey(), d.videoKey()) || q.getAnswers().size() != d.answers().size()) {
                return false;
            }
            for (int j = 0; j < q.getAnswers().size(); j++) {
                Answer a = q.getAnswers().get(j);
                CreateAnswerDTO da = d.answers().get(j);
                if (!a.getText().equals(da.text().trim()) || a.isCorrect() != da.correct()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static CreateTriviaDTO toDto(Trivia t) {
        TriviaDetailDTO d = TriviaMapper.toDetail(t, false, false);
        return new CreateTriviaDTO(d.title(), d.description(), d.category(), d.status(), d.playMode(), d.coverImageKey(),
                d.activityMinutes(),
                d.questions().stream().map(q -> new CreateQuestionDTO(null, q.type(), q.text(), q.timeLimitSeconds(),
                        q.basePoints(), q.imageKey(), q.videoKey(),
                        q.answers().stream().map(a -> new CreateAnswerDTO(a.text(), a.correct())).toList())).toList(),
                d.scoring(), d.events(), d.timeline());
    }

    private static <T> T or(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
