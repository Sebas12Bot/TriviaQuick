package com.triviaquick.controller;

import com.triviaquick.dto.ProfileDtos.*;
import com.triviaquick.exception.ApiError;
import com.triviaquick.security.AuthUser;
import com.triviaquick.security.TokenResolver;
import com.triviaquick.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Perfil del usuario autenticado. Solo opera sobre la propia cuenta (identidad del token). */
@RestController
@RequestMapping("/api/profile")
@Tag(name = "Perfil y preferencias")
public class ProfileController {

    private final ProfileService profileService;
    private final TokenResolver tokenResolver;

    public ProfileController(ProfileService profileService, TokenResolver tokenResolver) {
        this.profileService = profileService;
        this.tokenResolver = tokenResolver;
    }

    @GetMapping
    @Operation(summary = "Mi perfil y preferencias")
    public ProfileDTO get(@AuthenticationPrincipal AuthUser me) {
        return profileService.get(me);
    }

    @PatchMapping
    @Operation(summary = "Cambiar mi nombre visible")
    public ProfileDTO rename(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileDTO dto) {
        return profileService.rename(me, dto);
    }

    @PostMapping("/email")
    @Operation(summary = "Solicitar cambio de correo", description = "Requiere la contraseña actual; envía un enlace de verificación al correo nuevo.")
    @ApiResponse(responseCode = "409", description = "Correo en uso", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public EmailChangeResultDTO requestEmailChange(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangeEmailDTO dto) {
        return profileService.requestEmailChange(me, dto);
    }

    @DeleteMapping("/email")
    @Operation(summary = "Cancelar un cambio de correo pendiente")
    public ProfileDTO cancelEmailChange(@AuthenticationPrincipal AuthUser me) {
        return profileService.cancelEmailChange(me);
    }

    @PostMapping("/email/confirm")
    @Operation(summary = "Confirmar el cambio de correo con el token del enlace (no requiere sesión)")
    public ResponseEntity<Void> confirmEmail(@Valid @RequestBody ConfirmEmailDTO dto) {
        profileService.confirmEmailChange(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password")
    @Operation(summary = "Cambiar contraseña", description = "Invalida las sesiones anteriores y renueva la cookie de esta.")
    public ResponseEntity<ProfileDTO> changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordDTO dto) {
        ProfileService.TokenRefresh result = profileService.changePassword(me, dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenResolver.sessionCookie(result.newToken()).toString())
                .body(result.profile());
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir o cambiar mi foto de perfil (PNG o JPG, máx. 3 MB)")
    public ProfileDTO uploadAvatar(@AuthenticationPrincipal AuthUser me, @RequestParam("file") MultipartFile file) throws IOException {
        return profileService.uploadAvatar(me, file.getBytes());
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Eliminar mi foto de perfil")
    public ProfileDTO removeAvatar(@AuthenticationPrincipal AuthUser me) {
        return profileService.removeAvatar(me);
    }

    @PutMapping("/preferences")
    @Operation(summary = "Guardar mis preferencias de audio")
    public PreferencesDTO preferences(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PreferencesDTO dto) {
        return profileService.updatePreferences(me, dto);
    }

    @DeleteMapping
    @Operation(summary = "Eliminar mi cuenta", description = "Requiere la contraseña y escribir ELIMINAR. La cuenta se anonimiza; "
            + "el historial de partidas de otras personas se conserva.")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody DeleteAccountDTO dto) {
        profileService.deleteAccount(me, dto);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, tokenResolver.clearedCookie().toString())
                .build();
    }
}
