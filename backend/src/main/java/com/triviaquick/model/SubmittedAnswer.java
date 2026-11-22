package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Respuesta registrada por un participante. Única por participante y pregunta. */
@Entity
@Table(name = "submitted_answers")
public class SubmittedAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private SessionParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false)
    private Answer answer;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "points_awarded", nullable = false)
    private int pointsAwarded;

    @Column(name = "multiplier_applied", nullable = false)
    private boolean multiplierApplied;

    @Column(name = "response_time_ms", nullable = false)
    private int responseTimeMs;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "base_points", nullable = false)
    private int basePoints;

    @Column(name = "speed_bonus", nullable = false)
    private int speedBonus;

    @Column(name = "multiplier_factor", nullable = false)
    private int multiplierFactor = 1;

    protected SubmittedAnswer() {
    }

    public void recordBreakdown(int basePoints, int speedBonus, int multiplierFactor) {
        this.basePoints = basePoints;
        this.speedBonus = speedBonus;
        this.multiplierFactor = multiplierFactor;
    }

    public int getBasePoints() { return basePoints; }
    public int getSpeedBonus() { return speedBonus; }
    public int getMultiplierFactor() { return multiplierFactor; }

    public SubmittedAnswer(SessionParticipant participant, Question question, Answer answer, boolean correct,
                           int pointsAwarded, boolean multiplierApplied, int responseTimeMs, Instant submittedAt) {
        this.participant = participant;
        this.question = question;
        this.answer = answer;
        this.correct = correct;
        this.pointsAwarded = pointsAwarded;
        this.multiplierApplied = multiplierApplied;
        this.responseTimeMs = responseTimeMs;
        this.submittedAt = submittedAt;
    }

    public Long getId() { return id; }
    public SessionParticipant getParticipant() { return participant; }
    public Question getQuestion() { return question; }
    public Answer getAnswer() { return answer; }
    public boolean isCorrect() { return correct; }
    public int getPointsAwarded() { return pointsAwarded; }
    public boolean isMultiplierApplied() { return multiplierApplied; }
    public int getResponseTimeMs() { return responseTimeMs; }
    public Instant getSubmittedAt() { return submittedAt; }
}
