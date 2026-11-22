package com.triviaquick.model;

import jakarta.persistence.*;

/** Opción de respuesta de una pregunta. Solo el backend conoce cuál es la correcta. */
@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 200)
    private String text;

    @Column(nullable = false)
    private boolean correct;

    protected Answer() {
    }

    public Answer(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    void attachTo(Question question, int position) {
        this.question = question;
        this.position = position;
    }

    public Long getId() { return id; }
    public Question getQuestion() { return question; }
    public int getPosition() { return position; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
}
