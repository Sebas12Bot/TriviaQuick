package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trivia_id", nullable = false)
    private Trivia trivia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "join_code", nullable = false, unique = true, length = 8)
    private String joinCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.LOBBY;

    @Column(name = "current_question_index", nullable = false)
    private int currentQuestionIndex = -1;

    @Column(name = "question_started_at")
    private Instant questionStartedAt;

    @Column(name = "question_deadline")
    private Instant questionDeadline;

    @Column(name = "events_enabled", nullable = false)
    private boolean eventsEnabled;

    @Column(name = "event_frequency", nullable = false)
    private int eventFrequency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Version
    private long version;

    protected GameSession() {
    }

    public GameSession(Trivia trivia, User host, String joinCode, boolean eventsEnabled, int eventFrequency, Instant createdAt) {
        this.trivia = trivia;
        this.host = host;
        this.joinCode = joinCode;
        this.eventsEnabled = eventsEnabled;
        this.eventFrequency = eventFrequency;
        this.createdAt = createdAt;
    }

    public boolean isFinished() {
        return status == SessionStatus.FINISHED;
    }

    public boolean isHostedBy(Long userId) {
        return host.getId().equals(userId);
    }

    public void startQuestion(int index, Instant now, int timeLimitSeconds) {
        if (startedAt == null) {
            startedAt = now;
        }
        status = SessionStatus.QUESTION;
        currentQuestionIndex = index;
        questionStartedAt = now;
        questionDeadline = now.plusSeconds(timeLimitSeconds);
    }

    public void enterEvent() {
        status = SessionStatus.EVENT;
        questionDeadline = null;
    }

    public void finish(Instant now) {
        status = SessionStatus.FINISHED;
        finishedAt = now;
        questionDeadline = null;
    }

    public Long getId() { return id; }
    public Trivia getTrivia() { return trivia; }
    public User getHost() { return host; }
    public String getJoinCode() { return joinCode; }
    public SessionStatus getStatus() { return status; }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public Instant getQuestionStartedAt() { return questionStartedAt; }
    public Instant getQuestionDeadline() { return questionDeadline; }
    public boolean isEventsEnabled() { return eventsEnabled; }
    public int getEventFrequency() { return eventFrequency; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
