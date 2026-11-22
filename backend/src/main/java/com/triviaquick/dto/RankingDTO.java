package com.triviaquick.dto;

import com.triviaquick.model.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RankingDTO(
        Long sessionId,
        SessionStatus status,
        @Schema(description = "true mientras la partida no ha terminado") boolean provisional,
        String tieBreakRule,
        List<ParticipantRankingDTO> entries) {
}
