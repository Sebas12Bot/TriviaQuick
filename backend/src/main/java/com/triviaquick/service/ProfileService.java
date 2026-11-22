package com.triviaquick.service;

import com.triviaquick.dto.ProfileDtos.*;
import com.triviaquick.exception.ApiException;
import com.triviaquick.mapper.TriviaMapper;
import com.triviaquick.model.SessionParticipant;
import com.triviaquick.model.User;
import com.triviaquick.model.UserPreferences;
import com.triviaquick.repository.ParticipantRepository;
import com.triviaquick.repository.UserPreferencesRepository;
import com.triviaquick.repository.UserRepository;
import com.triviaquick.security.AuthUser;
import com.triviaquick.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Perfil del usuario autenticado. Todas las operaciones actúan sobre la identidad del token:
 * no existe ningún endpoint que reciba el id de otro usuario.
 */
@Service
public class ProfileService {

    private static final Duration EMAIL_TOKEN_TTL = Duration.ofMinutes(30);

    public record TokenRefresh(ProfileDTO profile, String newToken) {
    }

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final ParticipantRepository participants;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final MediaStorageService storage;
    private final MailService mail;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ProfileService(UserRepository users, UserPreferencesRepository preferences, ParticipantRepository participants,
                          PasswordEncoder encoder, JwtService jwtService, MediaStorageService storage, MailService mail,
                          Clock clock) {
        this.users = users;
        this.preferences = preferences;
        this.participants = participants;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.storage = storage;
        this.mail = mail;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileDTO get(AuthUser me) {
        return toDto(load(me));
    }

    @Transactional
    public ProfileDTO rename(AuthUser me, UpdateProfileDTO dto) {
        User user = load(me);
        user.rename(dto.displayName().trim());
        return toDto(user);
    }

    @Transactional
    public EmailChangeResultDTO requestEmailChange(AuthUser me, ChangeEmailDTO dto) {
        User user = load(me);
        requirePassword(user, dto.currentPassword());
        String newEmail = dto.newEmail().trim().toLowerCase(Locale.ROOT);
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw ApiException.badRequest("SAME_EMAIL", "Ese ya es tu correo actual.");
        }
        if (users.existsByEmailIgnoreCase(newEmail)) {
            throw ApiException.conflict("EMAIL_TAKEN", "Ya existe una cuenta con ese correo.");
        }
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        user.requestEmailChange(newEmail, sha256(token), clock.instant().plus(EMAIL_TOKEN_TTL));
        users.flush();
        boolean sent = mail.sendEmailVerification(newEmail, user.getDisplayName(), token);
        return new EmailChangeResultDTO(newEmail, sent);
    }

    /** Confirma el nuevo correo con el token del enlace. No requiere sesión: el token es la prueba. */
    @Transactional
    public void confirmEmailChange(ConfirmEmailDTO dto) {
        User user = users.findByEmailTokenHash(sha256(dto.token()))
                .orElseThrow(() -> ApiException.badRequest("INVALID_TOKEN", "El enlace de verificación no es válido o ya se usó."));
        if (user.getEmailTokenExpiresAt() == null || clock.instant().isAfter(user.getEmailTokenExpiresAt())) {
            user.clearEmailChange();
            throw ApiException.badRequest("TOKEN_EXPIRED", "El enlace de verificación caducó. Solicita el cambio de nuevo.");
        }
        if (users.existsByEmailIgnoreCase(user.getPendingEmail())) {
            user.clearEmailChange();
            throw ApiException.conflict("EMAIL_TAKEN", "Ese correo ya pertenece a otra cuenta.");
        }
        user.confirmEmailChange();
    }

    @Transactional
    public ProfileDTO cancelEmailChange(AuthUser me) {
        User user = load(me);
        user.clearEmailChange();
        return toDto(user);
    }

    /** Cambia la contraseña e invalida todas las sesiones anteriores; devuelve un token nuevo para esta. */
    @Transactional
    public TokenRefresh changePassword(AuthUser me, ChangePasswordDTO dto) {
        User user = load(me);
        requirePassword(user, dto.currentPassword());
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw ApiException.badRequest("PASSWORD_MISMATCH", "La confirmación no coincide con la nueva contraseña.");
        }
        if (encoder.matches(dto.newPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("PASSWORD_REUSED", "La nueva contraseña debe ser distinta de la actual.");
        }
        Instant now = clock.instant();
        user.changePassword(encoder.encode(dto.newPassword()), now);
        users.flush();
        return new TokenRefresh(toDto(user), jwtService.issueAfter(user.getId(), now));
    }

    @Transactional
    public ProfileDTO uploadAvatar(AuthUser me, byte[] data) {
        User user = load(me);
        String key = storage.storeImage(data, MediaStorageService.Kind.AVATAR);
        String previous = user.getAvatarKey();
        user.setAvatarKey(key);
        users.flush();
        if (previous != null) {
            storage.delete(previous);
        }
        return toDto(user);
    }

    @Transactional
    public ProfileDTO removeAvatar(AuthUser me) {
        User user = load(me);
        String previous = user.getAvatarKey();
        user.setAvatarKey(null);
        users.flush();
        if (previous != null) {
            storage.delete(previous);
        }
        return toDto(user);
    }

    @Transactional
    public PreferencesDTO updatePreferences(AuthUser me, PreferencesDTO dto) {
        UserPreferences prefs = preferences.findById(me.id())
                .orElseGet(() -> preferences.save(new UserPreferences(me.id(), clock.instant())));
        prefs.update(dto.soundEnabled(), dto.masterVolume(), dto.effectsVolume(), dto.countdownSounds(), clock.instant());
        return toDto(prefs);
    }

    /**
     * Elimina la cuenta por anonimización: la fila del usuario se conserva (sin datos personales y
     * desactivada) para no romper resultados, partidas ni cuestionarios ligados a otras personas.
     * El nombre mostrado en clasificaciones pasadas se sustituye por "Participante eliminado".
     */
    @Transactional
    public void deleteAccount(AuthUser me, DeleteAccountDTO dto) {
        User user = load(me);
        requirePassword(user, dto.currentPassword());
        String avatar = user.getAvatarKey();
        byte[] raw = new byte[24];
        random.nextBytes(raw);
        user.anonymize(encoder.encode(Base64.getEncoder().encodeToString(raw)), clock.instant());
        for (SessionParticipant p : participants.findByUserId(user.getId())) {
            p.anonymizeName();
        }
        preferences.findById(user.getId()).ifPresent(preferences::delete);
        users.flush();
        if (avatar != null) {
            storage.delete(avatar);
        }
    }

    private User load(AuthUser me) {
        User user = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("La cuenta no existe."));
        if (!user.isActive()) {
            throw ApiException.forbidden("La cuenta está desactivada.");
        }
        return user;
    }

    private void requirePassword(User user, String password) {
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw ApiException.badRequest("WRONG_PASSWORD", "La contraseña actual no es correcta.");
        }
    }

    private ProfileDTO toDto(User u) {
        return new ProfileDTO(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), TriviaMapper.mediaUrl(u.getAvatarKey()),
                u.getPendingEmail(), u.getCreatedAt(),
                preferences.findById(u.getId()).map(ProfileService::toDto).orElse(new PreferencesDTO(true, 70, 80, true)));
    }

    private static PreferencesDTO toDto(UserPreferences p) {
        return new PreferencesDTO(p.isSoundEnabled(), p.getMasterVolume(), p.getEffectsVolume(), p.isCountdownSounds());
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
