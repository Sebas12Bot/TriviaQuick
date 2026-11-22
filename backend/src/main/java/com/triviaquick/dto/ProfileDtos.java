package com.triviaquick.dto;

import com.triviaquick.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.Instant;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record ProfileDTO(Long id, String email, String displayName, Role role, String avatarUrl,
                             @Schema(description = "Correo pendiente de verificación, si lo hay") String pendingEmail,
                             Instant createdAt, PreferencesDTO preferences) {
    }

    public record UpdateProfileDTO(
            @NotBlank @Size(min = 2, max = 60)
            @Pattern(regexp = "^[\\p{L}\\p{N} .'_-]+$", message = "Usa solo letras, números, espacios y . ' _ -")
            String displayName) {
    }

    public record ChangeEmailDTO(
            @NotBlank @Email @Size(max = 160) String newEmail,
            @NotBlank @Size(max = 100) String currentPassword) {
    }

    public record ConfirmEmailDTO(@NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{20,100}$") String token) {
    }

    public record EmailChangeResultDTO(String pendingEmail,
                                       @Schema(description = "false en desarrollo sin SMTP: el enlace está en el log del servidor")
                                       boolean emailSent) {
    }

    public record ChangePasswordDTO(
            @NotBlank @Size(max = 100) String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres") String newPassword,
            @NotBlank @Size(max = 100) String confirmPassword) {
    }

    public record DeleteAccountDTO(
            @NotBlank @Size(max = 100) String currentPassword,
            @NotBlank @Pattern(regexp = "^ELIMINAR$", message = "Escribe ELIMINAR para confirmar") String confirmation) {
    }

    public record PreferencesDTO(
            @NotNull Boolean soundEnabled,
            @NotNull @Min(0) @Max(100) Integer masterVolume,
            @NotNull @Min(0) @Max(100) Integer effectsVolume,
            @NotNull Boolean countdownSounds) {
    }
}
