package com.triviaquick.service;

import com.triviaquick.config.AppProperties;
import com.triviaquick.dto.EventResponseDTO;
import com.triviaquick.dto.EventResponseResultDTO;
import com.triviaquick.dto.GameEventDTO;
import com.triviaquick.dto.PowerUpDTO;
import com.triviaquick.exception.ApiException;
import com.triviaquick.mapper.EventMapper;
import com.triviaquick.model.*;
import com.triviaquick.repository.*;
import com.triviaquick.security.AuthUser;
import com.triviaquick.security.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Minijuegos entre preguntas. Toda su lógica se ejecuta y valida aquí; el cliente solo envía
 * la opción elegida o la activación de una habilidad.
 *
 * <ul>
 *   <li><b>Reto relámpago</b>: reto breve para todos; acertarlo dentro del plazo suma una bonificación.</li>
 *   <li><b>Multiplicador</b>: se otorga a la mitad inferior de la clasificación (mecánica de remontada).
 *   Activado, duplica los puntos de la siguiente respuesta y se consume con ella, acierte o no.</li>
 *   <li><b>Escudo</b>: se otorga a quienes acertaron la pregunta anterior. Activado, bloquea la siguiente
 *   transferencia de puntos en su contra y se consume.</li>
 *   <li><b>Duelo</b>: enfrenta a los dos participantes con la menor diferencia de puntos entre puestos
 *   consecutivos. Gana quien acierte; si aciertan ambos, el más rápido. El ganador se lleva una cantidad
 *   limitada de puntos del perdedor (nunca más de los que tiene), salvo que el perdedor tenga un escudo activo.</li>
 * </ul>
 */
@Service
public class GameEventService {


    private final GameEventRepository events;
    private final GameEventResponseRepository responses;
    private final PowerUpRepository powerUps;
    private final ParticipantRepository participants;
    private final GameSessionRepository sessions;
    private final SubmittedAnswerRepository submittedAnswers;
    private final QuestionRepository questions;
    private final ScoreService scoreService;
    private final SessionAccessService access;
    private final SessionNotifier notifier;
    private final ChallengeBank challenges;
    private final RateLimiter rateLimiter;
    private final AppProperties.Game rules;
    private final Clock clock;

    public GameEventService(GameEventRepository events, GameEventResponseRepository responses,
                            PowerUpRepository powerUps, ParticipantRepository participants,
                            GameSessionRepository sessions, SubmittedAnswerRepository submittedAnswers,
                            QuestionRepository questions, ScoreService scoreService, SessionAccessService access,
                            SessionNotifier notifier, ChallengeBank challenges, RateLimiter rateLimiter,
                            AppProperties props, Clock clock) {
        this.events = events;
        this.responses = responses;
        this.powerUps = powerUps;
        this.participants = participants;
        this.sessions = sessions;
        this.submittedAnswers = submittedAnswers;
        this.questions = questions;
        this.scoreService = scoreService;
        this.access = access;
        this.notifier = notifier;
        this.challenges = challenges;
        this.rateLimiter = rateLimiter;
        this.rules = props.game();
        this.clock = clock;
    }

    /** Parámetros efectivos de un minijuego (configuración del cuestionario + ajustes del evento manual). */
    record EventParams(GameEventType type, int seconds, int reward, int factor, int stake,
                       ChallengeCategory category, PowerUpTarget target) {
    }

    /**
     * Si corresponde un minijuego tras la pregunta {@code afterIndex}, lo crea. Requiere la sesión bloqueada.
     * <ul>
     *   <li>Modo MANUAL: solo se ejecuta el evento que el docente colocó en ese hueco de la línea de tiempo.</li>
     *   <li>Modo AUTOMÁTICO: cada {@code frecuencia} preguntas, rotando entre los tipos habilitados,
     *   de modo que no se repite el mismo evento consecutivamente si hay más de uno habilitado.</li>
     *   <li>Desactivado: nunca.</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<GameEvent> maybeStart(GameSession lockedSession, int afterIndex, int questionCount) {
        boolean isLast = afterIndex >= questionCount - 1;
        Trivia trivia = lockedSession.getTrivia();
        EventConfig config = trivia.getEvents();
        if (!lockedSession.isEventsEnabled() || isLast || config.getMode() == EventMode.OFF) {
            return Optional.empty();
        }
        List<SessionParticipant> roster = participants.findBySessionIdOrderByJoinedAtAsc(lockedSession.getId());
        if (roster.isEmpty()) {
            return Optional.empty();
        }
        Optional<EventParams> params = config.getMode() == EventMode.MANUAL
                ? trivia.timelineEventAfter(afterIndex).map(t -> paramsFor(t.getType(), config, t))
                : autoParams(lockedSession, config, afterIndex, roster.size());
        if (params.isEmpty() || (params.get().type() == GameEventType.DUEL && roster.size() < 2)) {
            // Un duelo necesita dos participantes; no se sustituye por otro evento no configurado.
            return Optional.empty();
        }
        EventParams p = params.get();
        Instant now = clock.instant();
        GameEvent event = switch (p.type()) {
            case LIGHTNING_CHALLENGE -> startLightning(lockedSession, afterIndex, now, p);
            case MULTIPLIER -> startMultiplier(lockedSession, afterIndex, now, roster, p);
            case SHIELD -> startShield(lockedSession, afterIndex, now, roster, p);
            case DUEL -> startDuel(lockedSession, afterIndex, now, roster, p);
        };
        notifier.notify(lockedSession.getId(), SessionNotifier.Type.EVENT_STARTED, event.getExpiresAt(),
                Map.of("eventType", event.getType().name()));
        return Optional.of(event);
    }

    private Optional<EventParams> autoParams(GameSession session, EventConfig config, int afterIndex, int players) {
        if ((afterIndex + 1) % session.getEventFrequency() != 0) {
            return Optional.empty();
        }
        List<GameEventType> enabled = config.autoTypesInOrder().stream()
                .filter(t -> t != GameEventType.DUEL || players >= 2).toList();
        if (enabled.isEmpty()) {
            return Optional.empty();
        }
        Optional<GameEvent> last = events.findFirstBySessionIdOrderByStartedAtDescIdDesc(session.getId());
        int next = last.map(e -> (enabled.indexOf(e.getType()) + 1) % enabled.size()).orElse(0);
        if (next < 0) {
            next = 0;
        }
        return Optional.of(paramsFor(enabled.get(next), config, null));
    }

    static EventParams paramsFor(GameEventType type, EventConfig c, TimelineEvent t) {
        int seconds = switch (type) {
            case LIGHTNING_CHALLENGE -> c.getLightningSeconds();
            case DUEL -> c.getDuelSeconds();
            default -> c.getPowerUpSeconds();
        };
        PowerUpTarget target = switch (type) {
            case MULTIPLIER -> c.getMultiplierTarget();
            case SHIELD -> c.getShieldTarget();
            default -> null;
        };
        if (t != null) {
            seconds = t.getDurationSeconds() != null ? t.getDurationSeconds() : seconds;
            target = t.getTarget() != null ? t.getTarget() : target;
        }
        return new EventParams(type, seconds,
                t != null && t.getRewardPoints() != null ? t.getRewardPoints() : c.getLightningReward(),
                t != null && t.getMultiplierFactor() != null ? t.getMultiplierFactor() : c.getMultiplierFactor(),
                t != null && t.getStake() != null ? t.getStake() : c.getDuelStake(),
                t != null && t.getChallengeCategory() != null ? t.getChallengeCategory() : c.getLightningCategory(),
                target);
    }

    private GameEvent startLightning(GameSession session, int afterIndex, Instant now, EventParams p) {
        GameEvent e = new GameEvent(session, GameEventType.LIGHTNING_CHALLENGE, afterIndex, now, now.plusSeconds(p.seconds()));
        e.setChallengeKey(challenges.pick(p.category(), session.getId() * 31 + afterIndex).key());
        e.setParameters(p.reward(), null, null, null);
        e.setOutcome("Acierta el reto antes de que acabe el tiempo y suma " + p.reward() + " puntos.");
        return events.saveAndFlush(e);
    }

    private GameEvent startMultiplier(GameSession session, int afterIndex, Instant now, List<SessionParticipant> roster,
                                      EventParams p) {
        GameEvent e = new GameEvent(session, GameEventType.MULTIPLIER, afterIndex, now, now.plusSeconds(p.seconds()));
        e.setParameters(null, p.factor(), null, p.target());
        e = events.saveAndFlush(e);
        List<RankingService.RankedEntry> ranked = RankingService.rank(roster.stream().map(RankingService::toInput).toList());
        int recipients = p.target() == PowerUpTarget.ALL ? ranked.size() : Math.max(1, ranked.size() / 2);
        List<RankingService.RankedEntry> chosen = ranked.subList(ranked.size() - recipients, ranked.size());
        for (RankingService.RankedEntry entry : chosen) {
            SessionParticipant participant = participants.getReferenceById(entry.input().participantId());
            powerUps.save(new PlayerPowerUp(participant, PowerUpType.MULTIPLIER, e, now, p.factor()));
        }
        e.setOutcome(recipients + (recipients == 1 ? " participante recibió" : " participantes recibieron")
                + " un multiplicador x" + p.factor() + " para su próxima respuesta.");
        return e;
    }

    private GameEvent startShield(GameSession session, int afterIndex, Instant now, List<SessionParticipant> roster,
                                  EventParams p) {
        GameEvent e = new GameEvent(session, GameEventType.SHIELD, afterIndex, now, now.plusSeconds(p.seconds()));
        e.setParameters(null, null, null, p.target());
        e = events.saveAndFlush(e);
        Question previous = questions.findWithAnswersByTriviaAndPosition(session.getTrivia().getId(), afterIndex)
                .orElseThrow();
        int granted = 0;
        for (SessionParticipant participant : roster) {
            boolean eligible = p.target() == PowerUpTarget.ALL
                    || submittedAnswers.findByParticipantIdAndQuestionId(participant.getId(), previous.getId())
                    .map(SubmittedAnswer::isCorrect).orElse(false);
            if (eligible) {
                powerUps.save(new PlayerPowerUp(participant, PowerUpType.SHIELD, e, now, 1));
                granted++;
            }
        }
        String reason = p.target() == PowerUpTarget.ALL ? "." : " por acertar la pregunta anterior.";
        e.setOutcome(granted == 0
                ? "Nadie acertó la pregunta anterior: no se repartieron escudos."
                : granted + (granted == 1 ? " participante ganó" : " participantes ganaron") + " un escudo" + reason);
        return e;
    }

    private GameEvent startDuel(GameSession session, int afterIndex, Instant now, List<SessionParticipant> roster,
                                EventParams p) {
        List<RankingService.RankedEntry> ranked = RankingService.rank(roster.stream().map(RankingService::toInput).toList());
        int best = 0;
        int bestGap = Integer.MAX_VALUE;
        for (int i = 0; i + 1 < ranked.size(); i++) {
            int gap = ranked.get(i).input().score() - ranked.get(i + 1).input().score();
            if (gap < bestGap) {
                bestGap = gap;
                best = i;
            }
        }
        SessionParticipant first = participants.getReferenceById(ranked.get(best).input().participantId());
        SessionParticipant second = participants.getReferenceById(ranked.get(best + 1).input().participantId());
        GameEvent e = new GameEvent(session, GameEventType.DUEL, afterIndex, now, now.plusSeconds(p.seconds()));
        e.setDuelists(first, second);
        e.setParameters(null, null, p.stake(), null);
        e.setChallengeKey(challenges.pick(ChallengeCategory.ANY, session.getId() * 17 + afterIndex + 5).key());
        e.setOutcome(ranked.get(best).input().displayName() + " y " + ranked.get(best + 1).input().displayName()
                + " se enfrentan: quien gane se lleva hasta " + p.stake() + " puntos.");
        return events.saveAndFlush(e);
    }

    /** Respuesta de un participante a un reto relámpago o a un duelo. */
    @Transactional
    public EventResponseResultDTO respond(Long sessionId, Long eventId, AuthUser user, EventResponseDTO dto) {
        rateLimiter.check(user.id());
        GameSession session = sessions.findByIdForShare(sessionId)
                .orElseThrow(() -> ApiException.notFound("La sesión no existe."));
        SessionParticipant me = access.requireParticipant(sessionId, user);
        if (session.isFinished()) {
            throw ApiException.conflict("SESSION_FINISHED", "La partida ya terminó.");
        }
        // Bloquea el evento: serializa las respuestas de un duelo para que solo una lo resuelva.
        GameEvent event = events.findByIdForUpdate(eventId)
                .filter(e -> e.getSession().getId().equals(sessionId))
                .orElseThrow(() -> ApiException.notFound("El minijuego no existe en esta sesión."));
        Instant now = clock.instant();
        if (!event.acceptsResponsesAt(now)) {
            throw ApiException.conflict("EVENT_CLOSED", "Este minijuego ya no admite respuestas.");
        }
        if (event.getType() != GameEventType.LIGHTNING_CHALLENGE && event.getType() != GameEventType.DUEL) {
            throw ApiException.badRequest("EVENT_NOT_ANSWERABLE", "Este minijuego no requiere respuesta.");
        }
        if (event.getType() == GameEventType.DUEL && !event.isDuelist(me.getId())) {
            throw ApiException.forbidden("Solo los participantes del duelo pueden responder.");
        }
        if (responses.existsByEventIdAndParticipantId(eventId, me.getId())) {
            throw ApiException.conflict("EVENT_ALREADY_ANSWERED", "Ya respondiste este minijuego.");
        }
        ChallengeBank.Challenge challenge = challenges.find(event.getChallengeKey()).orElseThrow();
        if (dto.optionIndex() >= challenge.options().size()) {
            throw ApiException.badRequest("INVALID_OPTION", "La opción elegida no existe.");
        }
        boolean correct = challenge.isCorrect(dto.optionIndex());
        int elapsed = (int) Math.max(0, Duration.between(event.getStartedAt(), now).toMillis());
        responses.saveAndFlush(new GameEventResponse(event, me, dto.optionIndex(), correct, elapsed, now));

        SessionParticipant locked = participants.findByIdForUpdate(me.getId()).orElseThrow();
        int awarded = 0;
        String message;
        if (event.getType() == GameEventType.LIGHTNING_CHALLENGE) {
            if (correct) {
                scoreService.award(locked, rewardOf(event), ScoreReason.LIGHTNING_BONUS, null, event,
                        "Reto relámpago superado", "LIGHTNING:" + eventId + ":" + locked.getId());
                awarded = rewardOf(event);
                message = "¡Reto superado! +" + awarded + " puntos.";
            } else {
                message = "Respuesta incorrecta. Sin bonificación esta vez.";
            }
            notifier.notify(sessionId, SessionNotifier.Type.EVENT_UPDATED, event.getExpiresAt(),
                    Map.of("responses", responses.countByEventId(eventId)));
            notifier.notify(sessionId, SessionNotifier.Type.RANKING_UPDATED);
        } else {
            message = correct ? "Respuesta correcta. Esperando a tu rival…" : "Respuesta incorrecta.";
            if (responses.countByEventId(eventId) >= 2) {
                resolveDuel(event, now);
                message = event.getOutcome();
            } else {
                notifier.notify(sessionId, SessionNotifier.Type.EVENT_UPDATED, event.getExpiresAt(),
                        Map.of("responses", 1));
            }
        }
        return new EventResponseResultDTO(correct, awarded, locked.getScore(), message);
    }

    private int rewardOf(GameEvent event) {
        return event.getRewardPoints() != null ? event.getRewardPoints() : rules.lightningBonus();
    }

    /** Cierra el minijuego activo de la sesión (lo invoca el avance o el cierre de la partida). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void resolveActive(GameSession lockedSession) {
        Optional<GameEvent> active = events.findFirstBySessionIdAndStatusOrderByStartedAtDesc(
                lockedSession.getId(), GameEventStatus.ACTIVE);
        if (active.isEmpty()) {
            return;
        }
        GameEvent event = events.findByIdForUpdate(active.get().getId()).orElseThrow();
        if (!event.isActive()) {
            return;
        }
        Instant now = clock.instant();
        if (event.getType() == GameEventType.DUEL) {
            resolveDuel(event, now);
        } else {
            event.resolve(now, event.getOutcome());
            notifier.notify(lockedSession.getId(), SessionNotifier.Type.EVENT_RESOLVED);
        }
    }

    /** Resuelve un duelo exactamente una vez: el evento está bloqueado y su estado se comprueba antes. */
    private void resolveDuel(GameEvent lockedEvent, Instant now) {
        if (!lockedEvent.isActive()) {
            return;
        }
        Long firstId = lockedEvent.getDuelFirst().getId();
        Long secondId = lockedEvent.getDuelSecond().getId();
        List<GameEventResponse> rs = responses.findByEventId(lockedEvent.getId());
        Optional<GameEventResponse> r1 = rs.stream().filter(r -> r.getParticipant().getId().equals(firstId)).findFirst();
        Optional<GameEventResponse> r2 = rs.stream().filter(r -> r.getParticipant().getId().equals(secondId)).findFirst();

        Long winnerId = duelWinner(firstId, r1, secondId, r2);
        List<SessionParticipant> locked = participants.findAllByIdForUpdate(new ArrayList<>(List.of(firstId, secondId)));
        SessionParticipant first = locked.stream().filter(p -> p.getId().equals(firstId)).findFirst().orElseThrow();
        SessionParticipant second = locked.stream().filter(p -> p.getId().equals(secondId)).findFirst().orElseThrow();

        String outcome;
        if (winnerId == null) {
            outcome = "Duelo sin ganador: nadie acertó a tiempo. No hubo transferencia.";
        } else {
            SessionParticipant winner = winnerId.equals(firstId) ? first : second;
            SessionParticipant loser = winnerId.equals(firstId) ? second : first;
            ScoreService.TransferOutcome t = scoreService.transfer(loser, winner, lockedEvent.getStake() != null ? lockedEvent.getStake() : rules.duelStake(), lockedEvent,
                    "DUEL:" + lockedEvent.getId());
            if (t.blockedByShield()) {
                outcome = winner.getDisplayName() + " ganó el duelo, pero el escudo de " + loser.getDisplayName()
                        + " bloqueó la transferencia.";
            } else if (t.amount() == 0) {
                outcome = winner.getDisplayName() + " ganó el duelo; " + loser.getDisplayName()
                        + " no tenía puntos que transferir.";
            } else {
                outcome = winner.getDisplayName() + " ganó el duelo y se llevó " + t.amount() + " puntos de "
                        + loser.getDisplayName() + ".";
            }
        }
        lockedEvent.resolve(now, outcome);
        Long sessionId = lockedEvent.getSession().getId();
        notifier.notify(sessionId, SessionNotifier.Type.EVENT_RESOLVED, null, Map.of("outcome", outcome));
        notifier.notify(sessionId, SessionNotifier.Type.RANKING_UPDATED);
    }

    /** Gana quien acierte; si ambos aciertan, el más rápido. Si empatan en tiempo o nadie acierta, no hay ganador. */
    static Long duelWinner(Long firstId, Optional<GameEventResponse> r1, Long secondId, Optional<GameEventResponse> r2) {
        boolean c1 = r1.map(GameEventResponse::isCorrect).orElse(false);
        boolean c2 = r2.map(GameEventResponse::isCorrect).orElse(false);
        if (c1 && !c2) {
            return firstId;
        }
        if (c2 && !c1) {
            return secondId;
        }
        if (c1) {
            int t1 = r1.get().getResponseTimeMs();
            int t2 = r2.get().getResponseTimeMs();
            if (t1 == t2) {
                return null;
            }
            return t1 < t2 ? firstId : secondId;
        }
        return null;
    }

    /** El jugador activa una habilidad propia disponible. */
    @Transactional
    public PowerUpDTO activatePowerUp(Long sessionId, Long powerUpId, AuthUser user) {
        GameSession session = sessions.findByIdForShare(sessionId)
                .orElseThrow(() -> ApiException.notFound("La sesión no existe."));
        SessionParticipant me = access.requireParticipant(sessionId, user);
        if (session.isFinished()) {
            throw ApiException.conflict("SESSION_FINISHED", "La partida ya terminó.");
        }
        PlayerPowerUp powerUp = powerUps.findByIdForUpdate(powerUpId)
                .filter(p -> p.getParticipant().getId().equals(me.getId()))
                .orElseThrow(() -> ApiException.notFound("No tienes esa habilidad."));
        if (powerUp.getStatus() != PowerUpStatus.AVAILABLE) {
            throw ApiException.conflict("POWER_UP_NOT_AVAILABLE", "Esa habilidad ya fue activada o usada.");
        }
        if (!powerUps.findForUpdate(me.getId(), powerUp.getType(), PowerUpStatus.ACTIVE).isEmpty()) {
            throw ApiException.conflict("POWER_UP_ALREADY_ACTIVE", "Ya tienes una habilidad de este tipo activa.");
        }
        powerUp.activate(clock.instant());
        return EventMapper.toDto(powerUp);
    }

    @Transactional(readOnly = true)
    public Optional<GameEventDTO> currentFor(SessionAccessService.Viewer viewer) {
        Long sessionId = viewer.session().getId();
        Optional<GameEvent> event = viewer.session().getStatus() == SessionStatus.EVENT
                ? events.findFirstBySessionIdAndStatusOrderByStartedAtDesc(sessionId, GameEventStatus.ACTIVE)
                : Optional.empty();
        return event.map(e -> toViewerDto(e, viewer));
    }

    @Transactional(readOnly = true)
    public GameEventDTO currentForUser(Long sessionId, AuthUser user) {
        SessionAccessService.Viewer viewer = access.requireViewer(sessionId, user);
        return currentFor(viewer).orElseThrow(() -> ApiException.notFound("No hay ningún minijuego activo."));
    }

    @Transactional(readOnly = true)
    public List<PowerUpDTO> powerUpsOf(Long participantId) {
        return powerUps.findByParticipantIdOrderByGrantedAtAscIdAsc(participantId).stream()
                .sorted(Comparator.comparing(PlayerPowerUp::getGrantedAt))
                .map(EventMapper::toDto).toList();
    }

    private GameEventDTO toViewerDto(GameEvent e, SessionAccessService.Viewer viewer) {
        Long participantId = viewer.participant() == null ? null : viewer.participant().getId();
        boolean answerable = e.getType() == GameEventType.LIGHTNING_CHALLENGE || e.getType() == GameEventType.DUEL;
        boolean eligible = participantId != null && answerable && e.isActive()
                && (e.getType() == GameEventType.LIGHTNING_CHALLENGE || e.isDuelist(participantId));
        Optional<GameEventResponse> mine = participantId == null ? Optional.empty()
                : responses.findByEventIdAndParticipantId(e.getId(), participantId);
        ChallengeBank.Challenge challenge = e.getChallengeKey() == null ? null
                : challenges.find(e.getChallengeKey()).orElse(null);
        return EventMapper.toDto(e, challenge, eligible, mine.isPresent(), mine.map(GameEventResponse::isCorrect).orElse(null),
                responses.countByEventId(e.getId()));
    }
}
