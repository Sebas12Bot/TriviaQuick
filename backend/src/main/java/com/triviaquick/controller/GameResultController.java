package com.triviaquick.controller;

import com.triviaquick.dto.GameResultDTO;
import com.triviaquick.dto.PodiumDTO;
import com.triviaquick.dto.ScoreTransactionDTO;
import com.triviaquick.exception.ApiError;
import com.triviaquick.security.AuthUser;
import com.triviaquick.service.GameResultService;
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

import java.util.List;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
@Validated
@Tag(name = "Resultados")
public class GameResultController {

    private final GameResultService resultService;

    public GameResultController(GameResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/results")
    @Operation(summary = "Resultados definitivos (podio + clasificación completa)")
    @ApiResponse(responseCode = "409", description = "La partida aún no ha terminado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public GameResultDTO results(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return resultService.resultsFor(sessionId, user);
    }

    @GetMapping("/podium")
    @Operation(summary = "Los tres primeros puestos (con empates)")
    public PodiumDTO podium(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return resultService.podiumFor(sessionId, user);
    }

    @GetMapping("/score-transactions")
    @Operation(summary = "Historial de movimientos de puntos", description = "El anfitrión ve todos; un participante solo los suyos.")
    public List<ScoreTransactionDTO> transactions(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return resultService.transactionsFor(sessionId, user);
    }
}
