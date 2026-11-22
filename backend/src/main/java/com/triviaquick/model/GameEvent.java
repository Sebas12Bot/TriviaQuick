package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Minijuego que ocurre entre dos preguntas de una partida. */
@Entity
@Table(name = "game_events")
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameEventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameEventStatus status = GameEventStatus.ACTIVE;

    @Column(name = "after_question_index", nullable = false)
    private int afterQuestionIndex;

    @Column(name = "challenge_key", length = 40)
    private String challengeKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duel_first_id")
    private SessionParticipant duelFirst;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duel_second_id")
    private SessionParticipant duelSecond;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(length = 300)
    private String outcome;

    /** Parámetros aplicados en este evento (copiados de la configuración al crearlo). */
    @Column(name = "reward_points")
    private Integer rewardPoints;

    @Column(name = "multiplier_factor")
    private Integer multiplierFactor;

    @Column
    private Integer stake;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PowerUpTarget target;

    public void setParameters(Integer rewardPoints, Integer multiplierFactor, Integer stake, PowerUpTarget target) {
        this.rewardPoints = rewardPoints;
        this.multiplierFactor = multiplierFactor;
        this.stake = stake;
        this.target = target;
    }

    public Integer getRewardPoints() { return rewardPoints; }
    public Integer getMultiplierFactor() { return multiplierFactor; }
    public Integer getStake() { return stake; }
    public PowerUpTarget getTarget() { return target; }

    @Version
    private long version;

    protected GameEvent() {
    }

    public GameEvent(GameSession session, GameEventType type, int afterQuestionIndex, Instant startedAt, Instant expiresAt) {
        this.session = session;
        this.type = type;
        this.afterQuestionIndex = afterQuestionIndex;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return status == GameEventStatus.ACTIVE;
    }

    /** Un evento está vigente si sigue activo y no ha superado su plazo. */
    public boolean acceptsResponsesAt(Instant now) {
        return isActive() && !now.isAfter(expiresAt);
    }

    public boolean isDuelist(Long participantId) {
        return (duelFirst != null && duelFirst.getId().equals(participantId))
                || (duelSecond != null && duelSecond.getId().equals(participantId));
    }

    public void resolve(Instant now, String outcome) {
        if (!isActive()) {
            throw new IllegalStateException("El evento ya fue resuelto");
        }
        this.status = GameEventStatus.RESOLVED;
        this.resolvedAt = now;
        this.outcome = outcome;
    }

    public void setChallengeKey(String challengeKey) { this.challengeKey = challengeKey; }

    public void setDuelists(SessionParticipant first, SessionParticipant second) {
        this.duelFirst = first;
        this.duelSecond = second;
    }

    public void setOutcome(String outcome) { this.outcome = outcome; }

    public Long getId() { return id; }
    public GameSession getSession() { return session; }
    public GameEventType getType() { return type; }
    public GameEventStatus getStatus() { return status; }
    public int getAfterQuestionIndex() { return afterQuestionIndex; }
    public String getChallengeKey() { return challengeKey; }
    public SessionParticipant getDuelFirst() { return duelFirst; }
    public SessionParticipant getDuelSecond() { return duelSecond; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getOutcome() { return outcome; }
}
