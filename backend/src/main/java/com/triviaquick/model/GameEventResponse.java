package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Respuesta de un participante a un reto relámpago o a un duelo. Única por evento y participante. */
@Entity
@Table(name = "game_event_responses")
public class GameEventResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private GameEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private SessionParticipant participant;

    @Column(name = "option_index", nullable = false)
    private int optionIndex;

    @Column(nullable = false)
    private boolean correct;

    @Column(name = "response_time_ms", nullable = false)
    private int responseTimeMs;

    @Column(name = "responded_at", nullable = false)
    private Instant respondedAt;

    protected GameEventResponse() {
    }

    public GameEventResponse(GameEvent event, SessionParticipant participant, int optionIndex, boolean correct,
                             int responseTimeMs, Instant respondedAt) {
        this.event = event;
        this.participant = participant;
        this.optionIndex = optionIndex;
        this.correct = correct;
        this.responseTimeMs = responseTimeMs;
        this.respondedAt = respondedAt;
    }

    public Long getId() { return id; }
    public GameEvent getEvent() { return event; }
    public SessionParticipant getParticipant() { return participant; }
    public int getOptionIndex() { return optionIndex; }
    public boolean isCorrect() { return correct; }
    public int getResponseTimeMs() { return responseTimeMs; }
    public Instant getRespondedAt() { return respondedAt; }
}
