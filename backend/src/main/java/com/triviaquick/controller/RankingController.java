package com.triviaquick.controller;

import com.triviaquick.dto.RankingDTO;
import com.triviaquick.exception.ApiError;
import com.triviaquick.security.AuthUser;
import com.triviaquick.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
@Validated
@Tag(name = "Clasificación")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/ranking")
    @Operation(summary = "Clasificación de la sesión",
            description = "Provisional durante la partida; definitiva (persistida) al terminar. " + RankingService.TIE_BREAK_RULE)
    @ApiResponse(responseCode = "403", description = "No participas en la sesión", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public RankingDTO ranking(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return rankingService.rankingFor(sessionId, user);
    }
}
