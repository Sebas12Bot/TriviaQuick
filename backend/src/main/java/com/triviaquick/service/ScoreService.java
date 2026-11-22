package com.triviaquick.service;

import com.triviaquick.model.GameEvent;
import com.triviaquick.model.PlayerPowerUp;
import com.triviaquick.model.PowerUpStatus;
import com.triviaquick.model.PowerUpType;
import com.triviaquick.model.Question;
import com.triviaquick.model.ScoreReason;
import com.triviaquick.model.ScoreTransaction;
import com.triviaquick.model.SessionParticipant;
import com.triviaquick.repository.PowerUpRepository;
import com.triviaquick.repository.ScoreTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Único punto del sistema que modifica puntuaciones.
 *
 * <p>Invariantes:
 * <ul>
 *   <li>Cada cambio genera un {@link ScoreTransaction} con su saldo resultante, por lo que la suma de
 *   deltas de un participante coincide siempre con su puntuación.</li>
 *   <li>La puntuación nunca es negativa: una transferencia nunca supera el saldo disponible.</li>
 *   <li>Cada hecho de juego tiene una clave de idempotencia única: no puede aplicarse dos veces.</li>
 * </ul>
 * Los métodos exigen una transacción abierta y participantes ya bloqueados (SELECT ... FOR UPDATE)
 * por el llamador, de modo que dos operaciones concurrentes sobre el mismo jugador se serializan.
 */
@Service
public class ScoreService {

    private final ScoreTransactionRepository transactions;
    private final PowerUpRepository powerUps;
    private final Clock clock;

    public ScoreService(ScoreTransactionRepository transactions, PowerUpRepository powerUps, Clock clock) {
        this.transactions = transactions;
        this.powerUps = powerUps;
        this.clock = clock;
    }

    /** Porcentaje del tiempo de la pregunta consumido, medido con el reloj del servidor. */
    public static double elapsedPercent(int timeLimitSeconds, long responseMillis) {
        long limitMillis = timeLimitSeconds * 1000L;
        return Math.max(0, responseMillis) * 100.0 / limitMillis;
    }

    /** Desglose de una respuesta correcta: (base + velocidad) × multiplicador. */
    public record AnswerScore(int basePoints, int speedBonus, int factor) {
        public int total() {
            return (basePoints + speedBonus) * factor;
        }
    }

    /** Suma puntos (delta > 0) y registra el movimiento. */
    @Transactional(propagation = Propagation.MANDATORY)
    public ScoreTransaction award(SessionParticipant lockedParticipant, int points, ScoreReason reason,
                                  Question question, GameEvent event, String description, String idempotencyKey) {
        if (points <= 0) {
            throw new IllegalArgumentException("Una bonificación debe ser positiva");
        }
        return record(lockedParticipant, points, reason, question, event, null, description, idempotencyKey);
    }

    public record TransferOutcome(int amount, boolean blockedByShield) {
    }

    /**
     * Transfiere hasta {@code requestedAmount} puntos de {@code from} a {@code to}.
     * Si {@code from} tiene un escudo ACTIVO la transferencia se bloquea y el escudo se consume.
     * La cantidad real nunca supera el saldo de {@code from}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public TransferOutcome transfer(SessionParticipant lockedFrom, SessionParticipant lockedTo, int requestedAmount,
                                    GameEvent event, String idempotencyKeyBase) {
        if (requestedAmount <= 0) {
            throw new IllegalArgumentException("La cantidad a transferir debe ser positiva");
        }
        if (lockedFrom.getId().equals(lockedTo.getId())) {
            throw new IllegalArgumentException("Un participante no puede transferirse puntos a sí mismo");
        }
        Optional<PlayerPowerUp> shield = firstActive(lockedFrom.getId(), PowerUpType.SHIELD);
        if (shield.isPresent()) {
            shield.get().consume(clock.instant());
            record(lockedFrom, 0, ScoreReason.SHIELD_BLOCK, null, event, lockedTo,
                    "Escudo protector: bloqueó una transferencia de " + requestedAmount + " puntos",
                    idempotencyKeyBase + ":SHIELD");
            return new TransferOutcome(0, true);
        }
        int amount = Math.min(requestedAmount, lockedFrom.getScore());
        if (amount == 0) {
            return new TransferOutcome(0, false);
        }
        record(lockedFrom, -amount, ScoreReason.DUEL_LOSS, null, event, lockedTo,
                "Duelo perdido frente a " + lockedTo.getDisplayName(), idempotencyKeyBase + ":LOSS");
        record(lockedTo, amount, ScoreReason.DUEL_WIN, null, event, lockedFrom,
                "Duelo ganado frente a " + lockedFrom.getDisplayName(), idempotencyKeyBase + ":WIN");
        return new TransferOutcome(amount, false);
    }

    /** Consume el multiplicador activo, si lo hay, y devuelve su factor (1 si no había ninguno). */
    @Transactional(propagation = Propagation.MANDATORY)
    public int consumeActiveMultiplier(Long participantId) {
        Optional<PlayerPowerUp> multiplier = firstActive(participantId, PowerUpType.MULTIPLIER);
        multiplier.ifPresent(m -> m.consume(clock.instant()));
        return multiplier.map(PlayerPowerUp::getMultiplierFactor).orElse(1);
    }

    private Optional<PlayerPowerUp> firstActive(Long participantId, PowerUpType type) {
        List<PlayerPowerUp> active = powerUps.findForUpdate(participantId, type, PowerUpStatus.ACTIVE);
        return active.stream().findFirst();
    }

    private ScoreTransaction record(SessionParticipant participant, int delta, ScoreReason reason, Question question,
                                    GameEvent event, SessionParticipant counterpart, String description, String key) {
        if (transactions.existsByIdempotencyKey(key)) {
            throw new IllegalStateException("Movimiento de puntos ya aplicado: " + key);
        }
        participant.applyScoreDelta(delta);
        ScoreTransaction tx = new ScoreTransaction(participant, delta, participant.getScore(), reason, question,
                event, counterpart, truncate(description), key, clock.instant());
        return transactions.save(tx);
    }

    private static String truncate(String s) {
        return s.length() <= 200 ? s : s.substring(0, 200);
    }
}
