package com.triviaquick.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuración de minijuegos de un cuestionario: modo (automático, manual o desactivado)
 * y valores por defecto de cada habilidad. Los eventos manuales pueden sobrescribirlos.
 */
@Embeddable
public class EventConfig {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_mode", nullable = false, length = 20)
    private EventMode mode = EventMode.AUTO;

    @Column(name = "auto_frequency", nullable = false)
    private int autoFrequency = 2;

    @Column(name = "auto_event_types", nullable = false, length = 120)
    private String autoEventTypes = "LIGHTNING_CHALLENGE,MULTIPLIER,SHIELD,DUEL";

    @Column(name = "lightning_seconds", nullable = false)
    private int lightningSeconds = 20;

    @Column(name = "lightning_reward", nullable = false)
    private int lightningReward = 300;

    @Enumerated(EnumType.STRING)
    @Column(name = "lightning_category", nullable = false, length = 20)
    private ChallengeCategory lightningCategory = ChallengeCategory.ANY;

    @Column(name = "multiplier_factor", nullable = false)
    private int multiplierFactor = 2;

    @Enumerated(EnumType.STRING)
    @Column(name = "multiplier_target", nullable = false, length = 20)
    private PowerUpTarget multiplierTarget = PowerUpTarget.BOTTOM_HALF;

    @Enumerated(EnumType.STRING)
    @Column(name = "shield_target", nullable = false, length = 20)
    private PowerUpTarget shieldTarget = PowerUpTarget.CORRECT_PREVIOUS;

    @Column(name = "duel_seconds", nullable = false)
    private int duelSeconds = 20;

    @Column(name = "duel_stake", nullable = false)
    private int duelStake = 250;

    @Column(name = "power_up_seconds", nullable = false)
    private int powerUpSeconds = 15;

    public EventConfig() {
    }

    public void update(EventMode mode, int autoFrequency, Set<GameEventType> autoTypes, int lightningSeconds,
                       int lightningReward, ChallengeCategory lightningCategory, int multiplierFactor,
                       PowerUpTarget multiplierTarget, PowerUpTarget shieldTarget, int duelSeconds, int duelStake,
                       int powerUpSeconds) {
        this.mode = mode;
        this.autoFrequency = autoFrequency;
        this.autoEventTypes = autoTypes.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
        this.lightningSeconds = lightningSeconds;
        this.lightningReward = lightningReward;
        this.lightningCategory = lightningCategory;
        this.multiplierFactor = multiplierFactor;
        this.multiplierTarget = multiplierTarget;
        this.shieldTarget = shieldTarget;
        this.duelSeconds = duelSeconds;
        this.duelStake = duelStake;
        this.powerUpSeconds = powerUpSeconds;
    }

    /** Tipos habilitados para el modo automático, en un orden de rotación fijo. */
    public List<GameEventType> autoTypesInOrder() {
        Set<GameEventType> enabled = autoEventTypes == null || autoEventTypes.isBlank()
                ? EnumSet.noneOf(GameEventType.class)
                : Arrays.stream(autoEventTypes.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(GameEventType::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(GameEventType.class)));
        return List.of(GameEventType.LIGHTNING_CHALLENGE, GameEventType.MULTIPLIER, GameEventType.SHIELD, GameEventType.DUEL)
                .stream().filter(enabled::contains).toList();
    }

    public EventMode getMode() { return mode; }
    public int getAutoFrequency() { return autoFrequency; }
    public int getLightningSeconds() { return lightningSeconds; }
    public int getLightningReward() { return lightningReward; }
    public ChallengeCategory getLightningCategory() { return lightningCategory; }
    public int getMultiplierFactor() { return multiplierFactor; }
    public PowerUpTarget getMultiplierTarget() { return multiplierTarget; }
    public PowerUpTarget getShieldTarget() { return shieldTarget; }
    public int getDuelSeconds() { return duelSeconds; }
    public int getDuelStake() { return duelStake; }
    public int getPowerUpSeconds() { return powerUpSeconds; }
}
