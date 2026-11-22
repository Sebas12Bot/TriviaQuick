package com.triviaquick.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fila de la clasificación calculada por el servidor")
public record ParticipantRankingDTO(
        Long participantId,
        @Schema(description = "Posición; los empatados comparten posición (1, 2, 2, 4)") int position,
        String displayName,
        String initials,
        int score,
        int correctAnswers,
        @Schema(description = "Si esta fila corresponde a quien consulta") boolean currentUser,
        @Schema(description = "Si comparte posición con otro participante") boolean tied) {
}
