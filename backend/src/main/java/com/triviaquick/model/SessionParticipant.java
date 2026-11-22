package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Participante de una sesión. El campo {@code score} solo debe modificarse a través de
 * {@code ScoreService}, que registra cada cambio en {@link ScoreTransaction}.
 */
@Entity
@Table(name = "session_participants")
public class SessionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false)
    private int score;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "answered_count", nullable = false)
    private int answeredCount;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    @Column(name = "best_streak", nullable = false)
    private int bestStreak;

    @Column(name = "last_correct_index", nullable = false)
    private int lastCorrectIndex = -1;

    @Version
    private long version;

    protected SessionParticipant() {
    }

    public SessionParticipant(GameSession session, User user, Instant joinedAt) {
        this.session = session;
        this.user = user;
        this.displayName = user.getDisplayName();
        this.joinedAt = joinedAt;
    }

    /** Uso exclusivo de ScoreService: cambia el saldo validando que nunca sea negativo. */
    public void applyScoreDelta(int delta) {
        long next = (long) score + delta;
        if (next < 0) {
            throw new IllegalStateException("La puntuación no puede ser negativa");
        }
        if (next > Integer.MAX_VALUE) {
            throw new IllegalStateException("Desbordamiento de puntuación");
        }
        score = (int) next;
    }

    public void registerAnswer(boolean correct) {
        answeredCount++;
        if (correct) {
            correctAnswers++;
        }
    }

    /**
     * Actualiza la racha tras responder la pregunta {@code questionIndex} y devuelve la racha resultante.
     * Con {@code breaksOnMiss}, un fallo o una pregunta sin responder la reinician; sin esa opción,
     * la racha solo crece con cada acierto.
     */
    public int updateStreak(boolean correct, int questionIndex, boolean breaksOnMiss) {
        if (correct) {
            boolean consecutive = lastCorrectIndex == questionIndex - 1;
            currentStreak = (consecutive || !breaksOnMiss) ? currentStreak + 1 : 1;
            lastCorrectIndex = questionIndex;
            bestStreak = Math.max(bestStreak, currentStreak);
        } else if (breaksOnMiss) {
            currentStreak = 0;
        }
        return currentStreak;
    }

    public int getCurrentStreak() { return currentStreak; }
    public int getBestStreak() { return bestStreak; }

    /** Anonimiza el nombre mostrado en el historial cuando el usuario elimina su cuenta. */
    public void anonymizeName() {
        this.displayName = "Participante eliminado";
    }

    public Long getId() { return id; }
    public GameSession getSession() { return session; }
    public User getUser() { return user; }
    public String getDisplayName() { return displayName; }
    public int getScore() { return score; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getAnsweredCount() { return answeredCount; }
    public Instant getJoinedAt() { return joinedAt; }
}
