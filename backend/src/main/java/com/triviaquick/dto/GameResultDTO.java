package com.triviaquick.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Resultados definitivos de una sesión finalizada")
public record GameResultDTO(
        Long sessionId,
        String triviaTitle,
        Instant finishedAt,
        int participantCount,
        int questionCount,
        @Schema(description = "Fila de quien consulta; null si es el anfitrión") ParticipantRankingDTO myResult,
        PodiumDTO podium,
        List<ParticipantRankingDTO> ranking,
        String tieBreakRule,
        @Schema(description = "HOST si consulta el docente anfitrión; PARTICIPANT si es un participante") String viewerRole) {
}
