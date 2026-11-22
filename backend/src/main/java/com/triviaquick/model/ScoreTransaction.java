package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Movimiento de puntos inmutable. La suma de deltas de un participante es igual a su puntuación. */
@Entity
@Table(name = "score_transactions")
public class ScoreTransaction {

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
    private int delta;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScoreReason reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private GameEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterpart_participant_id")
    private SessionParticipant counterpart;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ScoreTransaction() {
    }

    public ScoreTransaction(SessionParticipant participant, int delta, int balanceAfter, ScoreReason reason,
                            Question question, GameEvent event, SessionParticipant counterpart,
                            String description, String idempotencyKey, Instant createdAt) {
        this.session = participant.getSession();
        this.participant = participant;
        this.delta = delta;
        this.balanceAfter = balanceAfter;
        this.reason = reason;
        this.question = question;
        this.event = event;
        this.counterpart = counterpart;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public GameSession getSession() { return session; }
    public SessionParticipant getParticipant() { return participant; }
    public int getDelta() { return delta; }
    public int getBalanceAfter() { return balanceAfter; }
    public ScoreReason getReason() { return reason; }
    public Question getQuestion() { return question; }
    public GameEvent getEvent() { return event; }
    public SessionParticipant getCounterpart() { return counterpart; }
    public String getDescription() { return description; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
}
