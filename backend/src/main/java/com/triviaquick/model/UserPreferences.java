package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

/** Preferencias personales (audio) de un usuario. */
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "sound_enabled", nullable = false)
    private boolean soundEnabled = true;

    @Column(name = "master_volume", nullable = false)
    private int masterVolume = 70;

    @Column(name = "effects_volume", nullable = false)
    private int effectsVolume = 80;

    @Column(name = "countdown_sounds", nullable = false)
    private boolean countdownSounds = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreferences() {
    }

    public UserPreferences(Long userId, Instant now) {
        this.userId = userId;
        this.updatedAt = now;
    }

    public void update(boolean soundEnabled, int masterVolume, int effectsVolume, boolean countdownSounds, Instant now) {
        this.soundEnabled = soundEnabled;
        this.masterVolume = masterVolume;
        this.effectsVolume = effectsVolume;
        this.countdownSounds = countdownSounds;
        this.updatedAt = now;
    }

    public Long getUserId() { return userId; }
    public boolean isSoundEnabled() { return soundEnabled; }
    public int getMasterVolume() { return masterVolume; }
    public int getEffectsVolume() { return effectsVolume; }
    public boolean isCountdownSounds() { return countdownSounds; }
    public Instant getUpdatedAt() { return updatedAt; }
}
