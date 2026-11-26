package com.triviaquick.dto;

import com.triviaquick.model.QuestionType;
import com.triviaquick.model.ScoreReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Reporte de un participante en una sesión: respuesta por respuesta y bonificaciones")
public record ParticipantReportDTO(
        Long participantId,
        String displayName,
        String initials,
        @Schema(description = "Posición actual (definitiva si la partida terminó)") int position,
        int score,
        int correctAnswers,
        int wrongAnswers,
        @Schema(description = "Preguntas ya planteadas que no respondió") int unanswered,
        int bestStreak,
        @Schema(description = "Tiempo medio de las respuestas, en milisegundos (null si no respondió ninguna)") Long averageResponseMs,
        List<QuestionReportDTO> questions,
        List<BonusDTO> bonuses) {

    public record QuestionReportDTO(
            int index,
            String text,
            QuestionType type,
            @Schema(description = "ANSWERED_CORRECT, ANSWERED_WRONG, UNANSWERED o NOT_ASKED") String outcome,
            String chosenAnswer,
            String correctAnswer,
            int pointsAwarded,
            int speedBonus,
            int multiplierFactor,
            Integer responseTimeMs,
            @Schema(description = "Si fue la respuesta correcta más rápida de la pregunta") boolean fastest) {
    }

    public record BonusDTO(ScoreReason reason, int delta, String description) {
    }
}
