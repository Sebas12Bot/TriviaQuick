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
    private final com.triviaquick.service.ParticipantReportService reportService;

    public GameResultController(GameResultService resultService, com.triviaquick.service.ParticipantReportService reportService) {
        this.resultService = resultService;
        this.reportService = reportService;
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

    @GetMapping("/participants/{participantId}/report")
    @Operation(summary = "Reporte de un participante: respuestas correctas e incorrectas, puntos y bonificaciones",
            description = "Lo ven el docente anfitrión y el propio participante. Otros estudiantes reciben 403.")
    @ApiResponse(responseCode = "403", description = "Sin permiso", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public com.triviaquick.dto.ParticipantReportDTO report(@PathVariable @Positive Long sessionId,
                                                           @PathVariable @Positive Long participantId,
                                                           @AuthenticationPrincipal AuthUser user) {
        return reportService.report(sessionId, participantId, user);
    }

    @GetMapping("/score-transactions")
    @Operation(summary = "Historial de movimientos de puntos", description = "El anfitrión ve todos; un participante solo los suyos.")
    public List<ScoreTransactionDTO> transactions(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return resultService.transactionsFor(sessionId, user);
    }
}
