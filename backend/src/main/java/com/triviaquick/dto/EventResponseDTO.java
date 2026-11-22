package com.triviaquick.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Respuesta a un reto relámpago o a un duelo: solo el índice de la opción elegida. */
public record EventResponseDTO(@NotNull @Min(0) @Max(7) Integer optionIndex) {
}
