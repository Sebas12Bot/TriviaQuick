package com.triviaquick.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "avatar_key", length = 80)
    private String avatarKey;

    @Column(name = "pending_email", length = 160)
    private String pendingEmail;

    /** SHA-256 del token de verificación de correo; el token en claro nunca se guarda. */
    @Column(name = "email_token_hash", length = 64)
    private String emailTokenHash;

    @Column(name = "email_token_expires_at")
    private Instant emailTokenExpiresAt;

    /** Los JWT emitidos antes de este instante dejan de ser válidos (cambio de contraseña o borrado). */
    @Column(name = "credentials_changed_at")
    private Instant credentialsChangedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private long version;

    protected User() {
    }

    public User(String email, String displayName, String passwordHash, Role role, Instant createdAt) {
        this.email = email;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public void rename(String displayName) {
        this.displayName = displayName;
    }

    public void changePassword(String newHash, Instant now) {
        this.passwordHash = newHash;
        this.credentialsChangedAt = now;
    }

    public void setAvatarKey(String avatarKey) {
        this.avatarKey = avatarKey;
    }

    public void requestEmailChange(String newEmail, String tokenHash, Instant expiresAt) {
        this.pendingEmail = newEmail;
        this.emailTokenHash = tokenHash;
        this.emailTokenExpiresAt = expiresAt;
    }

    public void confirmEmailChange() {
        this.email = pendingEmail;
        clearEmailChange();
    }

    public void clearEmailChange() {
        this.pendingEmail = null;
        this.emailTokenHash = null;
        this.emailTokenExpiresAt = null;
    }

    /** Anonimiza la cuenta: conserva la fila para no romper el historial ajeno. */
    public void anonymize(String placeholderHash, Instant now) {
        this.email = "eliminado-" + id + "@cuenta-eliminada.invalid";
        this.displayName = "Cuenta eliminada";
        this.passwordHash = placeholderHash;
        this.avatarKey = null;
        clearEmailChange();
        this.active = false;
        this.deletedAt = now;
        this.credentialsChangedAt = now;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public String getAvatarKey() { return avatarKey; }
    public String getPendingEmail() { return pendingEmail; }
    public String getEmailTokenHash() { return emailTokenHash; }
    public Instant getEmailTokenExpiresAt() { return emailTokenExpiresAt; }
    public Instant getCredentialsChangedAt() { return credentialsChangedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setActive(boolean active) { this.active = active; }
}
