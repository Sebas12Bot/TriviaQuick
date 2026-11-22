package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Habilidad de un participante (multiplicador o escudo).
 * Ciclo de vida: AVAILABLE → ACTIVE (el jugador la activa) → CONSUMED (se usa una sola vez).
 */
@Entity
@Table(name = "player_power_ups")
public class PlayerPowerUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private SessionParticipant participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PowerUpType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PowerUpStatus status = PowerUpStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_event_id", nullable = false)
    private GameEvent sourceEvent;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Version
    private long version;

    @Column(name = "multiplier_factor", nullable = false)
    private int multiplierFactor = 2;

    protected PlayerPowerUp() {
    }

    public PlayerPowerUp(SessionParticipant participant, PowerUpType type, GameEvent sourceEvent, Instant grantedAt,
                         int multiplierFactor) {
        this(participant, type, sourceEvent, grantedAt);
        this.multiplierFactor = multiplierFactor;
    }

    public int getMultiplierFactor() { return multiplierFactor; }

    public PlayerPowerUp(SessionParticipant participant, PowerUpType type, GameEvent sourceEvent, Instant grantedAt) {
        this.participant = participant;
        this.type = type;
        this.sourceEvent = sourceEvent;
        this.grantedAt = grantedAt;
    }

    public void activate(Instant now) {
        if (status != PowerUpStatus.AVAILABLE) {
            throw new IllegalStateException("La habilidad no está disponible");
        }
        status = PowerUpStatus.ACTIVE;
        activatedAt = now;
    }

    public void consume(Instant now) {
        if (status != PowerUpStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede consumir una habilidad activa");
        }
        status = PowerUpStatus.CONSUMED;
        consumedAt = now;
    }

    public Long getId() { return id; }
    public SessionParticipant getParticipant() { return participant; }
    public PowerUpType getType() { return type; }
    public PowerUpStatus getStatus() { return status; }
    public GameEvent getSourceEvent() { return sourceEvent; }
    public Instant getGrantedAt() { return grantedAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getConsumedAt() { return consumedAt; }
}
