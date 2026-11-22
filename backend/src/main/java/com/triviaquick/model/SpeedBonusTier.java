package com.triviaquick.model;

import jakarta.persistence.*;

/** Tramo de bonificación por velocidad: respuestas dentro del X % del tiempo ganan +N puntos. */
@Entity
@Table(name = "speed_bonus_tiers")
public class SpeedBonusTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trivia_id", nullable = false)
    private Trivia trivia;

    @Column(name = "max_elapsed_percent", nullable = false)
    private int maxElapsedPercent;

    @Column(name = "bonus_points", nullable = false)
    private int bonusPoints;

    protected SpeedBonusTier() {
    }

    public SpeedBonusTier(Trivia trivia, int maxElapsedPercent, int bonusPoints) {
        this.trivia = trivia;
        this.maxElapsedPercent = maxElapsedPercent;
        this.bonusPoints = bonusPoints;
    }

    public Long getId() { return id; }
    public int getMaxElapsedPercent() { return maxElapsedPercent; }
    public int getBonusPoints() { return bonusPoints; }
}
