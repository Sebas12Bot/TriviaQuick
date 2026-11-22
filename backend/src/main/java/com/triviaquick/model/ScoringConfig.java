package com.triviaquick.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Reglas de puntuación de un cuestionario (los tramos viven en sus propias tablas). */
@Embeddable
public class ScoringConfig {

    @Column(name = "default_points", nullable = false)
    private int defaultPoints = 1000;

    @Column(name = "speed_bonus_enabled", nullable = false)
    private boolean speedBonusEnabled = true;

    @Column(name = "streak_enabled", nullable = false)
    private boolean streakEnabled = true;

    /** Si es true, fallar o no responder una pregunta rompe la racha. */
    @Column(name = "streak_breaks_on_miss", nullable = false)
    private boolean streakBreaksOnMiss = true;

    public ScoringConfig() {
    }

    public ScoringConfig(int defaultPoints, boolean speedBonusEnabled, boolean streakEnabled, boolean streakBreaksOnMiss) {
        this.defaultPoints = defaultPoints;
        this.speedBonusEnabled = speedBonusEnabled;
        this.streakEnabled = streakEnabled;
        this.streakBreaksOnMiss = streakBreaksOnMiss;
    }

    public int getDefaultPoints() { return defaultPoints; }
    public boolean isSpeedBonusEnabled() { return speedBonusEnabled; }
    public boolean isStreakEnabled() { return streakEnabled; }
    public boolean isStreakBreaksOnMiss() { return streakBreaksOnMiss; }
}
