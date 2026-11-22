package com.triviaquick.controller;

import com.triviaquick.dto.AuthDtos.LoginRequestDTO;
import com.triviaquick.dto.AuthDtos.RegisterRequestDTO;
import com.triviaquick.dto.AuthDtos.UserDTO;
import com.triviaquick.exception.ApiError;
import com.triviaquick.security.AuthUser;
import com.triviaquick.security.TokenResolver;
import com.triviaquick.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** El token viaja en una cookie httpOnly; nunca en el cuerpo de la respuesta. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;
    private final TokenResolver tokenResolver;

    public AuthController(AuthService authService, TokenResolver tokenResolver) {
        this.authService = authService;
        this.tokenResolver = tokenResolver;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar una cuenta", description = "Las cuentas de docente requieren el código de alta configurado en el servidor.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada; la sesión queda iniciada mediante cookie httpOnly")
    @ApiResponse(responseCode = "409", description = "Correo ya registrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        AuthService.AuthResult result = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, tokenResolver.sessionCookie(result.token()).toString())
                .body(result.user());
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada mediante cookie httpOnly")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<UserDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        AuthService.AuthResult result = authService.login(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenResolver.sessionCookie(result.token()).toString())
                .body(result.user());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, tokenResolver.clearedCookie().toString())
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario autenticado")
    public UserDTO me(@AuthenticationPrincipal AuthUser user) {
        return new UserDTO(user.id(), user.email(), user.displayName(), user.role());
    }
}
