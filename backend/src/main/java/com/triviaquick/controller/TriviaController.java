package com.triviaquick.controller;

import com.triviaquick.dto.TriviaDtos.CreateTriviaDTO;
import com.triviaquick.dto.TriviaDtos.TriviaDetailDTO;
import com.triviaquick.dto.TriviaDtos.TriviaSummaryDTO;
import com.triviaquick.exception.ApiError;
import com.triviaquick.security.AuthUser;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.triviaquick.service.TriviaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trivias")
@PreAuthorize("hasRole('TEACHER')")
@Validated
@Tag(name = "Trivias", description = "Solo docentes")
public class TriviaController {

    private final TriviaService triviaService;

    public TriviaController(TriviaService triviaService) {
        this.triviaService = triviaService;
    }

    @GetMapping
    @Operation(summary = "Mis trivias")
    public List<TriviaSummaryDTO> mine(@AuthenticationPrincipal AuthUser user) {
        return triviaService.listMine(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una trivia con sus preguntas y respuestas")
    public TriviaDetailDTO create(@Valid @RequestBody CreateTriviaDTO dto, @AuthenticationPrincipal AuthUser user) {
        return triviaService.create(dto, user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una trivia propia (incluye respuestas correctas)")
    public TriviaDetailDTO get(@PathVariable @Positive Long id, @AuthenticationPrincipal AuthUser user) {
        return triviaService.get(id, user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Guardar un cuestionario completo (borrador o publicado)",
            description = "Reemplaza preguntas, puntuación y línea de tiempo. 409 si hay una partida en curso, "
                    + "o si ya se jugó y se intentan cambiar las preguntas (QUESTIONS_LOCKED).")
    @ApiResponse(responseCode = "409", description = "En uso o con preguntas bloqueadas",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public TriviaDetailDTO update(@PathVariable @Positive Long id, @Valid @RequestBody CreateTriviaDTO dto,
                                  @AuthenticationPrincipal AuthUser user) {
        return triviaService.update(id, dto, user);
    }

    @PostMapping("/{id}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Duplicar un cuestionario como borrador editable")
    public TriviaDetailDTO duplicate(@PathVariable @Positive Long id, @AuthenticationPrincipal AuthUser user) {
        return triviaService.duplicate(id, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un cuestionario que nunca se ha jugado")
    public void delete(@PathVariable @Positive Long id, @AuthenticationPrincipal AuthUser user) {
        triviaService.delete(id, user);
    }
}
