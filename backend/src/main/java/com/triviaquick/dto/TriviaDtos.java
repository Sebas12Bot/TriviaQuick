package com.triviaquick.dto;

import com.triviaquick.model.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * DTO del editor de cuestionarios. El documento se envía completo (crear o actualizar) y es
 * compatible con el formato anterior ({@code title}, {@code description}, {@code questions}).
 */
public final class TriviaDtos {

    private TriviaDtos() {
    }

    private static final String IMAGE_KEY = "^[a-f0-9]{32}\\.(png|jpg)$";
    private static final String VIDEO_KEY = "^[a-f0-9]{32}\\.mp4$";

    public record CreateAnswerDTO(
            @NotBlank @Size(max = 200) String text,
            @NotNull Boolean correct) {
    }

    public record CreateQuestionDTO(
            @Schema(description = "Id de una pregunta existente (solo informativo)") Long id,
            @Schema(defaultValue = "MULTIPLE_CHOICE") QuestionType type,
            @NotBlank @Size(max = 500) String text,
            @NotNull @Min(5) @Max(120) Integer timeLimitSeconds,
            @NotNull @Min(10) @Max(5000) Integer basePoints,
            @Pattern(regexp = IMAGE_KEY, message = "Imagen no válida") String imageKey,
            @Pattern(regexp = VIDEO_KEY, message = "Video no válido") String videoKey,
            @NotEmpty @Size(min = 2, max = 6) List<@Valid @NotNull CreateAnswerDTO> answers) {
    }

    public record SpeedTierDTO(
            @NotNull @Min(1) @Max(100) Integer maxElapsedPercent,
            @NotNull @Min(0) @Max(2000) Integer bonusPoints) {
    }

    public record StreakTierDTO(
            @NotNull @Min(2) @Max(50) Integer streakLength,
            @NotNull @Min(1) @Max(2000) Integer bonusPoints) {
    }

    public record ScoringDTO(
            @Min(10) @Max(5000) Integer defaultPoints,
            Boolean speedBonusEnabled,
            @Size(max = 6) List<@Valid @NotNull SpeedTierDTO> speedTiers,
            Boolean streakEnabled,
            Boolean streakBreaksOnMiss,
            @Size(max = 6) List<@Valid @NotNull StreakTierDTO> streakTiers,
            @Schema(description = "Bonus para quien acierta cada pregunta en menos tiempo") Boolean fastestBonusEnabled,
            @Min(1) @Max(2000) Integer fastestBonusPoints) {
    }

    public record AbilityConfigDTO(
            @Min(5) @Max(60) Integer lightningSeconds,
            @Min(10) @Max(2000) Integer lightningReward,
            ChallengeCategory lightningCategory,
            @Min(2) @Max(3) Integer multiplierFactor,
            PowerUpTarget multiplierTarget,
            PowerUpTarget shieldTarget,
            @Min(5) @Max(60) Integer duelSeconds,
            @Min(10) @Max(2000) Integer duelStake,
            @Min(5) @Max(60) Integer powerUpSeconds) {
    }

    public record EventSettingsDTO(
            EventMode mode,
            @Min(1) @Max(5) Integer autoFrequency,
            Set<GameEventType> autoTypes,
            @Valid AbilityConfigDTO abilities) {
    }

    @Schema(description = "Evento manual: se ejecuta después de la pregunta afterQuestion (índice desde 0)")
    public record TimelineEventDTO(
            @NotNull @Min(0) @Max(49) Integer afterQuestion,
            @NotNull GameEventType type,
            @Min(5) @Max(60) Integer durationSeconds,
            @Min(10) @Max(2000) Integer rewardPoints,
            @Min(2) @Max(3) Integer multiplierFactor,
            @Min(10) @Max(2000) Integer stake,
            ChallengeCategory challengeCategory,
            PowerUpTarget target) {
    }

    public record CreateTriviaDTO(
            @NotBlank @Size(max = 140) String title,
            @Size(max = 500) String description,
            @Size(max = 60) String category,
            @Schema(description = "DRAFT guarda sin publicar; por compatibilidad, si se omite se publica") TriviaStatus status,
            PlayMode playMode,
            @Pattern(regexp = IMAGE_KEY, message = "Imagen no válida") String coverImageKey,
            @Min(1) @Max(240) Integer activityMinutes,
            @NotNull @Size(max = 50) List<@Valid @NotNull CreateQuestionDTO> questions,
            @Valid ScoringDTO scoring,
            @Valid EventSettingsDTO events,
            @Size(max = 49) List<@Valid @NotNull TimelineEventDTO> timeline) {
    }

    public record TriviaSummaryDTO(Long id, String title, String description, String category, TriviaStatus status,
                                   PlayMode playMode, int questionCount, String coverImageUrl, Instant createdAt,
                                   Instant updatedAt, long sessionsPlayed, boolean questionsLocked) {
    }

    /** Vista para el docente propietario, incluida la respuesta correcta. */
    public record AnswerDetailDTO(Long id, String text, boolean correct) {
    }

    public record QuestionDetailDTO(Long id, int position, QuestionType type, String text, int timeLimitSeconds,
                                    int basePoints, String imageKey, String imageUrl, String videoKey, String videoUrl,
                                    List<AnswerDetailDTO> answers) {
    }

    public record TriviaDetailDTO(Long id, String title, String description, String category, TriviaStatus status,
                                  PlayMode playMode, String coverImageKey, String coverImageUrl, Integer activityMinutes,
                                  Instant createdAt, Instant updatedAt,
                                  @Schema(description = "true si ya hay partidas: las preguntas no se pueden modificar")
                                  boolean questionsLocked,
                                  @Schema(description = "true si hay una partida en curso: no se puede editar") boolean inUse,
                                  List<QuestionDetailDTO> questions, ScoringDTO scoring, EventSettingsDTO events,
                                  List<TimelineEventDTO> timeline) {
    }
}
