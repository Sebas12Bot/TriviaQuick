package com.triviaquick.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Podio: posiciones 1 a 3. Con empates un puesto puede tener varios participantes y puede faltar un puesto (1, 1, 3).")
public record PodiumDTO(Long sessionId, List<PodiumPlaceDTO> places) {

    public record PodiumPlaceDTO(int position, int score, List<ParticipantRankingDTO> participants) {
    }
}
