package com.triviaquick.model;

import jakarta.persistence.*;

/**
 * Evento colocado manualmente por el docente en la línea de tiempo, después de la pregunta
 * {@code afterQuestion} (índice base 0). Los campos nulos usan los valores del cuestionario.
 */
@Entity
@Table(name = "trivia_timeline_events")
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trivia_id", nullable = false)
    private Trivia trivia;

    @Column(name = "after_question", nullable = false)
    private int afterQuestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameEventType type;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "reward_points")
    private Integer rewardPoints;

    @Column(name = "multiplier_factor")
    private Integer multiplierFactor;

    @Column
    private Integer stake;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_category", length = 20)
    private ChallengeCategory challengeCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PowerUpTarget target;

    protected TimelineEvent() {
    }

    public TimelineEvent(Trivia trivia, int afterQuestion, GameEventType type, Integer durationSeconds,
                         Integer rewardPoints, Integer multiplierFactor, Integer stake,
                         ChallengeCategory challengeCategory, PowerUpTarget target) {
        this.trivia = trivia;
        this.afterQuestion = afterQuestion;
        this.type = type;
        this.durationSeconds = durationSeconds;
        this.rewardPoints = rewardPoints;
        this.multiplierFactor = multiplierFactor;
        this.stake = stake;
        this.challengeCategory = challengeCategory;
        this.target = target;
    }

    public Long getId() { return id; }
    public int getAfterQuestion() { return afterQuestion; }
    public GameEventType getType() { return type; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getRewardPoints() { return rewardPoints; }
    public Integer getMultiplierFactor() { return multiplierFactor; }
    public Integer getStake() { return stake; }
    public ChallengeCategory getChallengeCategory() { return challengeCategory; }
    public PowerUpTarget getTarget() { return target; }
}
