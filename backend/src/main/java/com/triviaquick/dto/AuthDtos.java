package com.triviaquick.dto;

import com.triviaquick.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO de autenticación. Nunca devuelven contraseñas ni tokens en el cuerpo. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequestDTO(
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(max = 100) String password) {
    }

    public record RegisterRequestDTO(
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 2, max = 60)
            @Pattern(regexp = "^[\\p{L}\\p{N} .'_-]+$", message = "Usa solo letras, números, espacios y . ' _ -")
            String displayName,
            @NotBlank @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
            String password,
            @NotNull Role role,
            @Schema(description = "Código de alta de docentes; obligatorio si role = TEACHER")
            @Size(max = 100) String teacherCode) {
    }

    @Schema(description = "Usuario autenticado")
    public record UserDTO(Long id, String email, String displayName, Role role) {
    }
}
