package com.triviaquick.dto;

import com.triviaquick.model.GameEventStatus;
import com.triviaquick.model.GameEventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Minijuego activo o último resuelto, proyectado para quien lo consulta")
public record GameEventDTO(
        Long id,
        GameEventType type,
        GameEventStatus status,
        int afterQuestionIndex,
        Instant startedAt,
        Instant expiresAt,
        @Schema(description = "Reto (relámpago o duelo); sin la respuesta correcta") ChallengeDTO challenge,
        @Schema(description = "Si quien consulta puede responder este evento") boolean eligible,
        boolean responded,
        @Schema(description = "Resultado de la respuesta propia, solo cuando ya respondió") Boolean myResponseCorrect,
        List<String> duelists,
        long responsesCount,
        String outcome) {

    public record ChallengeDTO(String category, String prompt, List<String> options) {
    }
}
