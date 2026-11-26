package com.triviaquick.dto;

import com.triviaquick.model.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public final class SessionDtos {

    private SessionDtos() {
    }

    public enum ViewerRole { HOST, PARTICIPANT }

    public record CreateSessionDTO(
            @NotNull @Positive Long triviaId,
            @Schema(description = "Activa los minijuegos entre preguntas", defaultValue = "true") Boolean eventsEnabled,
            @Schema(description = "Cada cuántas preguntas aparece un minijuego", defaultValue = "2")
            @Min(1) @Max(5) Integer eventFrequency) {
    }

    public record JoinSessionDTO(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{6}$", message = "El código tiene 6 caracteres") String code) {
    }

    public record SessionSummaryDTO(Long id, String triviaTitle, SessionStatus status, ViewerRole role,
                                    @Schema(description = "Solo visible para el anfitrión") String joinCode,
                                    long participantCount, Instant createdAt, Instant finishedAt) {
    }

    public record OptionDTO(Long id, String text) {
    }

    @Schema(description = "Pregunta en curso. No incluye la respuesta correcta.")
    public record QuestionViewDTO(Long id, int index, int total, String text, List<OptionDTO> options,
                                  int timeLimitSeconds, int basePoints, Instant startedAt, Instant deadline,
                                  com.triviaquick.model.QuestionType type, String imageUrl, String videoUrl) {
    }

    public record MyAnswerDTO(Long answerId, boolean correct, int pointsAwarded, boolean multiplierApplied,
                              int speedBonus, int multiplierFactor) {
    }

    public record ParticipantBriefDTO(Long participantId, String displayName, String initials) {
    }

    public record MyStandingDTO(Long participantId, String displayName, int score, int correctAnswers,
                                int answeredCount, int position, int currentStreak, int bestStreak) {
    }

    @Schema(description = "Estado de la sesión proyectado para quien lo consulta")
    public record SessionStateDTO(
            Long id,
            String triviaTitle,
            SessionStatus status,
            ViewerRole role,
            @Schema(description = "Solo visible para el anfitrión") String joinCode,
            Instant serverTime,
            int questionCount,
            QuestionViewDTO currentQuestion,
            MyAnswerDTO myAnswer,
            @Schema(description = "Solo anfitrión: respuestas recibidas en la pregunta actual") Long answersReceived,
            GameEventDTO currentEvent,
            MyStandingDTO me,
            List<PowerUpDTO> myPowerUps,
            List<ParticipantBriefDTO> participants,
            boolean lastQuestion) {
    }

    public record SubmitAnswerDTO(
            @NotNull @Positive Long questionId,
            @NotNull @Positive Long answerId) {
    }

    @Schema(description = "Resultado calculado por el servidor para una respuesta")
    public record AnswerResultDTO(boolean correct, int pointsAwarded, boolean multiplierApplied, int totalScore,
                                  Long correctAnswerId,
                                  @Schema(description = "Puntos base de la pregunta (0 si falló)") int basePoints,
                                  @Schema(description = "Bonificación por velocidad según el tramo") int speedBonus,
                                  @Schema(description = "Factor del multiplicador aplicado (1 = ninguno)") int multiplierFactor,
                                  @Schema(description = "Racha actual tras esta respuesta") int streak,
                                  @Schema(description = "Bonificación por racha obtenida con esta respuesta") int streakBonus) {
    }
}
