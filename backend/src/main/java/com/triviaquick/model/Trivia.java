package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Cuestionario creado por un docente: preguntas, reglas de puntuación y secuencia de eventos. */
@Entity
@Table(name = "trivias")
public class Trivia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(length = 60)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TriviaStatus status = TriviaStatus.PUBLISHED;

    @Enumerated(EnumType.STRING)
    @Column(name = "play_mode", nullable = false, length = 20)
    private PlayMode playMode = PlayMode.GROUP;

    @Column(name = "cover_image_key", length = 80)
    private String coverImageKey;

    @Column(name = "activity_minutes")
    private Integer activityMinutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private long version;

    @Embedded
    private ScoringConfig scoring = new ScoringConfig();

    @Embedded
    private EventConfig events = new EventConfig();

    @OneToMany(mappedBy = "trivia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "trivia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("maxElapsedPercent ASC")
    private List<SpeedBonusTier> speedTiers = new ArrayList<>();

    @OneToMany(mappedBy = "trivia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("streakLength ASC")
    private List<StreakBonusTier> streakTiers = new ArrayList<>();

    @OneToMany(mappedBy = "trivia", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("afterQuestion ASC")
    private List<TimelineEvent> timeline = new ArrayList<>();

    protected Trivia() {
    }

    public Trivia(String title, String description, User owner, Instant createdAt) {
        this.title = title;
        this.description = description;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void addQuestion(Question question) {
        question.attachTo(this, questions.size());
        questions.add(question);
    }

    public void updateDetails(String title, String description, String category, TriviaStatus status, PlayMode playMode,
                              String coverImageKey, Integer activityMinutes, ScoringConfig scoring, Instant now) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = status;
        this.playMode = playMode;
        this.coverImageKey = coverImageKey;
        this.activityMinutes = activityMinutes;
        this.scoring = scoring;
        this.updatedAt = now;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    /** Puntos extra por velocidad para una respuesta que consumió {@code elapsedPercent} del tiempo. */
    public int speedBonusFor(double elapsedPercent) {
        if (!scoring.isSpeedBonusEnabled() || elapsedPercent > 100) {
            return 0;
        }
        return speedTiers.stream()
                .sorted(Comparator.comparingInt(SpeedBonusTier::getMaxElapsedPercent))
                .filter(t -> elapsedPercent <= t.getMaxElapsedPercent())
                .map(SpeedBonusTier::getBonusPoints)
                .findFirst().orElse(0);
    }

    /** Bonificación al alcanzar exactamente {@code streak} aciertos seguidos, si hay un tramo con esa longitud. */
    public int streakBonusFor(int streak) {
        if (!scoring.isStreakEnabled()) {
            return 0;
        }
        return streakTiers.stream().filter(t -> t.getStreakLength() == streak)
                .map(StreakBonusTier::getBonusPoints).findFirst().orElse(0);
    }

    public Optional<TimelineEvent> timelineEventAfter(int questionIndex) {
        return timeline.stream().filter(e -> e.getAfterQuestion() == questionIndex).findFirst();
    }

    public boolean isPublished() {
        return status == TriviaStatus.PUBLISHED;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public TriviaStatus getStatus() { return status; }
    public PlayMode getPlayMode() { return playMode; }
    public String getCoverImageKey() { return coverImageKey; }
    public Integer getActivityMinutes() { return activityMinutes; }
    public User getOwner() { return owner; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public ScoringConfig getScoring() { return scoring; }
    public EventConfig getEvents() { return events; }
    public List<Question> getQuestions() { return questions; }
    public List<SpeedBonusTier> getSpeedTiers() { return speedTiers; }
    public List<StreakBonusTier> getStreakTiers() { return streakTiers; }
    public List<TimelineEvent> getTimeline() { return timeline; }
}
