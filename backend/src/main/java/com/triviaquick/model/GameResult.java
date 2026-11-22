package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Resultado definitivo e inmutable de un participante, generado al cerrar la sesión. */
@Entity
@Table(name = "game_results")
public class GameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private SessionParticipant participant;

    @Column(nullable = false)
    private int position;

    @Column(name = "final_score", nullable = false)
    private int finalScore;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "answered_count", nullable = false)
    private int answeredCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GameResult() {
    }

    public GameResult(GameSession session, SessionParticipant participant, int position, int finalScore,
                      int correctAnswers, int answeredCount, Instant createdAt) {
        this.session = session;
        this.participant = participant;
        this.position = position;
        this.finalScore = finalScore;
        this.correctAnswers = correctAnswers;
        this.answeredCount = answeredCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public GameSession getSession() { return session; }
    public SessionParticipant getParticipant() { return participant; }
    public int getPosition() { return position; }
    public int getFinalScore() { return finalScore; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getAnsweredCount() { return answeredCount; }
    public Instant getCreatedAt() { return createdAt; }
}
