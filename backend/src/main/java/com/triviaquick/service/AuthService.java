package com.triviaquick.service;

import com.triviaquick.config.AppProperties;
import com.triviaquick.dto.AuthDtos.LoginRequestDTO;
import com.triviaquick.dto.AuthDtos.RegisterRequestDTO;
import com.triviaquick.dto.AuthDtos.UserDTO;
import com.triviaquick.exception.ApiException;
import com.triviaquick.model.Role;
import com.triviaquick.model.User;
import com.triviaquick.repository.UserRepository;
import com.triviaquick.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Locale;

@Service
public class AuthService {

    public record AuthResult(UserDTO user, String token) {
    }

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AppProperties.Security securityProps;
    private final Clock clock;
    /** Hash ficticio: iguala el coste de la verificación cuando el correo no existe (evita enumerar cuentas). */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwtService, AppProperties props,
                       Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.securityProps = props.security();
        this.clock = clock;
        this.dummyHash = encoder.encode("triviaquick-dummy-password");
    }

    @Transactional
    public AuthResult register(RegisterRequestDTO dto) {
        if (dto.role() == Role.TEACHER && !teacherCodeMatches(dto.teacherCode())) {
            throw ApiException.forbidden("El código de docente no es válido.");
        }
        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "Ya existe una cuenta con ese correo.");
        }
        User user = users.save(new User(email, dto.displayName().trim(), encoder.encode(dto.password()), dto.role(),
                clock.instant()));
        return new AuthResult(toDto(user), jwtService.issue(user.getId()));
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequestDTO dto) {
        User user = users.findByEmailIgnoreCase(dto.email().trim()).orElse(null);
        boolean ok = encoder.matches(dto.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !ok || !user.isActive()) {
            throw new BadCredentialsException("invalid");
        }
        return new AuthResult(toDto(user), jwtService.issue(user.getId()));
    }

    public static UserDTO toDto(User u) {
        return new UserDTO(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole());
    }

    private boolean teacherCodeMatches(String provided) {
        String expected = securityProps.teacherSignupCode();
        if (expected == null || expected.isBlank() || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }
}
