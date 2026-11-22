package com.triviaquick.controller;

import com.triviaquick.dto.SessionDtos.*;
import com.triviaquick.exception.ApiError;
import com.triviaquick.security.AuthUser;
import com.triviaquick.service.GameSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Recibe peticiones, valida la entrada con DTO y delega en {@link GameSessionService}. */
@RestController
@RequestMapping("/api/sessions")
@Validated
@Tag(name = "Sesiones de juego")
public class GameSessionController {

    private final GameSessionService sessionService;

    public GameSessionController(GameSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Crear una partida a partir de una trivia propia (docente)")
    public SessionSummaryDTO create(@Valid @RequestBody CreateSessionDTO dto, @AuthenticationPrincipal AuthUser user) {
        return sessionService.create(dto, user);
    }

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Unirse a una partida con su código (estudiante)")
    @ApiResponse(responseCode = "404", description = "Código inexistente", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public SessionSummaryDTO join(@Valid @RequestBody JoinSessionDTO dto, @AuthenticationPrincipal AuthUser user) {
        return sessionService.join(dto, user);
    }

    @GetMapping
    @Operation(summary = "Partidas en las que participo o que he creado")
    public List<SessionSummaryDTO> mine(@AuthenticationPrincipal AuthUser user) {
        return sessionService.listMine(user);
    }

    @GetMapping("/{sessionId}/state")
    @Operation(summary = "Estado de la partida para quien consulta", description = "Nunca incluye la respuesta correcta de la pregunta en curso.")
    @ApiResponse(responseCode = "403", description = "No participas en la sesión", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public SessionStateDTO state(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return sessionService.stateFor(sessionId, user);
    }

    @PostMapping("/{sessionId}/start")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Iniciar la partida (anfitrión)")
    public SessionStateDTO start(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return sessionService.start(sessionId, user);
    }

    @PostMapping("/{sessionId}/advance")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Avanzar a la siguiente fase (anfitrión)",
            description = "Pregunta → minijuego o siguiente pregunta; minijuego → siguiente pregunta; tras la última, cierra la partida.")
    public SessionStateDTO advance(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return sessionService.advance(sessionId, user);
    }

    @PostMapping("/{sessionId}/finish")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Cerrar la partida y generar los resultados definitivos (anfitrión)")
    public SessionStateDTO finish(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return sessionService.finish(sessionId, user);
    }

    @PostMapping("/{sessionId}/answers")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Registrar una respuesta",
            description = "El cliente envía solo la pregunta y la opción. El servidor valida y calcula los puntos.")
    @ApiResponse(responseCode = "200", description = "Respuesta registrada y puntuada por el servidor")
    @ApiResponse(responseCode = "409", description = "Respuesta duplicada, tiempo agotado o partida finalizada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "429", description = "Demasiadas respuestas seguidas", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public AnswerResultDTO answer(@PathVariable @Positive Long sessionId, @Valid @RequestBody SubmitAnswerDTO dto,
                                  @AuthenticationPrincipal AuthUser user) {
        return sessionService.submitAnswer(sessionId, user, dto);
    }
}
