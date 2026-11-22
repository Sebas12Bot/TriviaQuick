package com.triviaquick.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Error devuelto por la API. Nunca incluye trazas ni detalles internos.")
public record ApiError(
        @Schema(example = "409") int status,
        @Schema(example = "ANSWER_ALREADY_SUBMITTED") String code,
        @Schema(example = "Ya respondiste esta pregunta.") String message,
        @Schema(description = "Errores por campo, si la validación falló") Map<String, String> fieldErrors,
        Instant timestamp) {

    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, Map.of(), Instant.now());
    }
}
