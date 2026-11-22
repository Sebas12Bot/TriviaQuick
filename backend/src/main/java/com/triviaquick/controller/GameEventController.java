package com.triviaquick.controller;

import com.triviaquick.dto.EventResponseDTO;
import com.triviaquick.dto.EventResponseResultDTO;
import com.triviaquick.dto.GameEventDTO;
import com.triviaquick.dto.PowerUpDTO;
import com.triviaquick.exception.ApiError;
import com.triviaquick.security.AuthUser;
import com.triviaquick.service.GameEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
@Validated
@Tag(name = "Minijuegos y habilidades")
public class GameEventController {

    private final GameEventService eventService;

    public GameEventController(GameEventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/events/current")
    @Operation(summary = "Minijuego activo")
    @ApiResponse(responseCode = "404", description = "No hay minijuego activo", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public GameEventDTO current(@PathVariable @Positive Long sessionId, @AuthenticationPrincipal AuthUser user) {
        return eventService.currentForUser(sessionId, user);
    }

    @PostMapping("/events/{eventId}/responses")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Responder un reto relámpago o un duelo", description = "Solo se envía el índice de la opción; el servidor decide el resultado.")
    @ApiResponse(responseCode = "409", description = "Minijuego cerrado o ya respondido", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public EventResponseResultDTO respond(@PathVariable @Positive Long sessionId, @PathVariable @Positive Long eventId,
                                          @Valid @RequestBody EventResponseDTO dto, @AuthenticationPrincipal AuthUser user) {
        return eventService.respond(sessionId, eventId, user, dto);
    }

    @PostMapping("/power-ups/{powerUpId}/activate")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Activar una habilidad propia (multiplicador o escudo)")
    public PowerUpDTO activate(@PathVariable @Positive Long sessionId, @PathVariable @Positive Long powerUpId,
                               @AuthenticationPrincipal AuthUser user) {
        return eventService.activatePowerUp(sessionId, powerUpId, user);
    }
}
