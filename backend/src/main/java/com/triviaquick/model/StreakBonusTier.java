package com.triviaquick.model;

import jakarta.persistence.*;

/** Tramo de racha: al encadenar N aciertos seguidos se suman M puntos (una vez por tramo y racha). */
@Entity
@Table(name = "streak_bonus_tiers")
public class StreakBonusTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trivia_id", nullable = false)
    private Trivia trivia;

    @Column(name = "streak_length", nullable = false)
    private int streakLength;

    @Column(name = "bonus_points", nullable = false)
    private int bonusPoints;

    protected StreakBonusTier() {
    }

    public StreakBonusTier(Trivia trivia, int streakLength, int bonusPoints) {
        this.trivia = trivia;
        this.streakLength = streakLength;
        this.bonusPoints = bonusPoints;
    }

    public Long getId() { return id; }
    public int getStreakLength() { return streakLength; }
    public int getBonusPoints() { return bonusPoints; }
}
