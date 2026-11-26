package com.triviaquick.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trivia_id", nullable = false)
    private Trivia trivia;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "time_limit_seconds", nullable = false)
    private int timeLimitSeconds;

    @Column(name = "base_points", nullable = false)
    private int basePoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType type = QuestionType.MULTIPLE_CHOICE;

    @Column(name = "image_key", length = 80)
    private String imageKey;

    @Column(name = "video_key", length = 80)
    private String videoKey;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Answer> answers = new ArrayList<>();

    protected Question() {
    }

    public Question(String text, int timeLimitSeconds, int basePoints) {
        this.text = text;
        this.timeLimitSeconds = timeLimitSeconds;
        this.basePoints = basePoints;
    }

    public Question(String text, int timeLimitSeconds, int basePoints, QuestionType type, String imageKey) {
        this(text, timeLimitSeconds, basePoints);
        this.type = type;
        this.imageKey = imageKey;
    }

    public QuestionType getType() { return type; }
    public String getImageKey() { return imageKey; }
    public String getVideoKey() { return videoKey; }
    public void setVideoKey(String videoKey) { this.videoKey = videoKey; }

    void attachTo(Trivia trivia, int position) {
        this.trivia = trivia;
        this.position = position;
    }

    public void addAnswer(Answer answer) {
        answer.attachTo(this, answers.size());
        answers.add(answer);
    }

    public Optional<Answer> findAnswer(Long answerId) {
        return answers.stream().filter(a -> a.getId().equals(answerId)).findFirst();
    }

    public Long getId() { return id; }
    public Trivia getTrivia() { return trivia; }
    public int getPosition() { return position; }
    public String getText() { return text; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public int getBasePoints() { return basePoints; }
    public List<Answer> getAnswers() { return answers; }
}
