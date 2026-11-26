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

    /** Bonus para quien acierta cada pregunta en menos tiempo (se otorga al cerrar la pregunta). */
    @Column(name = "fastest_bonus_enabled", nullable = false)
    private boolean fastestBonusEnabled = true;

    @Column(name = "fastest_bonus_points", nullable = false)
    private int fastestBonusPoints = 100;

    public ScoringConfig() {
    }

    public ScoringConfig(int defaultPoints, boolean speedBonusEnabled, boolean streakEnabled, boolean streakBreaksOnMiss) {
        this(defaultPoints, speedBonusEnabled, streakEnabled, streakBreaksOnMiss, true, 100);
    }

    public ScoringConfig(int defaultPoints, boolean speedBonusEnabled, boolean streakEnabled, boolean streakBreaksOnMiss,
                         boolean fastestBonusEnabled, int fastestBonusPoints) {
        this.defaultPoints = defaultPoints;
        this.speedBonusEnabled = speedBonusEnabled;
        this.streakEnabled = streakEnabled;
        this.streakBreaksOnMiss = streakBreaksOnMiss;
        this.fastestBonusEnabled = fastestBonusEnabled;
        this.fastestBonusPoints = fastestBonusPoints;
    }

    public int getDefaultPoints() { return defaultPoints; }
    public boolean isSpeedBonusEnabled() { return speedBonusEnabled; }
    public boolean isStreakEnabled() { return streakEnabled; }
    public boolean isStreakBreaksOnMiss() { return streakBreaksOnMiss; }
    public boolean isFastestBonusEnabled() { return fastestBonusEnabled; }
    public int getFastestBonusPoints() { return fastestBonusPoints; }
}
