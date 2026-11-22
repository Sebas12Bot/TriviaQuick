package com.triviaquick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "triviaquick")
public record AppProperties(
        Db db,
        Jwt jwt,
        Cookie cookie,
        Cors cors,
        Security security,
        RateLimit rateLimit,
        Game game,
        Seed seed,
        EmbeddedDb embeddedDb,
        Storage storage,
        Mail mail) {

    public record Storage(String dir, long maxUploadBytes) {
    }

    /** {@code publicUrl}: URL del frontend usada en los enlaces de los correos de verificación. */
    public record Mail(String from, String publicUrl) {
    }

    public record Db(int poolSize) {
    }

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cookie(String name, boolean secure, String sameSite) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Security(String teacherSignupCode) {
    }

    public record RateLimit(int answersPerWindow, int windowSeconds) {
    }

    public record Game(int lightningBonus, int duelStake, int lightningSeconds, int duelSeconds,
                       int powerUpSeconds, long answerGraceMillis) {
    }

    public record Seed(boolean demoData, String demoPassword) {
    }

    public record EmbeddedDb(int port, String dataDir, boolean clean) {
    }
}
