package com.triviaquick.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexión a PostgreSQL.
 * <ul>
 *   <li>Con {@code DB_URL} definido se usa ese servidor (producción). Acepta {@code jdbc:postgresql://...}
 *   o el formato de los proveedores en la nube {@code postgres://usuario:clave@host:5432/base}.</li>
 *   <li>Sin {@code DB_URL} se inicia un PostgreSQL embebido (binarios reales) para desarrollo y pruebas.</li>
 * </ul>
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
    private static final String EMBEDDED_DB_NAME = "triviaquick";

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(@Value("${spring.datasource.url:}") String url,
                                       @Value("${spring.datasource.username:}") String username,
                                       @Value("${spring.datasource.password:}") String password,
                                       AppProperties props) throws IOException, SQLException {
        HikariConfig config = new HikariConfig();
        config.setPoolName("triviaquick-pool");
        config.setMaximumPoolSize(props.db().poolSize());

        EmbeddedPostgres embedded = null;
        if (url == null || url.isBlank()) {
            embedded = startEmbedded(props.embeddedDb());
            config.setJdbcUrl(embedded.getJdbcUrl("postgres", EMBEDDED_DB_NAME));
            config.setUsername("postgres");
            config.setPassword("");
        } else {
            JdbcTarget target = normalize(url.trim(), username, password);
            config.setJdbcUrl(target.jdbcUrl());
            config.setUsername(target.username());
            config.setPassword(target.password());
            log.info("Conectando a PostgreSQL externo: {}", target.jdbcUrl().replaceAll("\\?.*$", ""));
        }
        final EmbeddedPostgres toStop = embedded;
        return new HikariDataSource(config) {
            @Override
            public void close() {
                super.close();
                if (toStop != null) {
                    try {
                        toStop.close();
                    } catch (IOException e) {
                        log.warn("No se pudo detener PostgreSQL embebido: {}", e.getMessage());
                    }
                }
            }
        };
    }

    private EmbeddedPostgres startEmbedded(AppProperties.EmbeddedDb settings) throws IOException, SQLException {
        EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder();
        if (settings.port() > 0) {
            builder.setPort(settings.port());
        }
        if (settings.dataDir() != null && !settings.dataDir().isBlank()) {
            Path dataDir = Path.of(settings.dataDir()).toAbsolutePath();
            Files.createDirectories(dataDir.getParent());
            builder.setDataDirectory(dataDir);
            builder.setCleanDataDirectory(settings.clean());
        }
        log.info("DB_URL no está definido: iniciando PostgreSQL embebido (solo desarrollo)");
        EmbeddedPostgres pg = builder.start();
        try (Connection c = pg.getPostgresDatabase().getConnection()) {
            boolean exists;
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                ps.setString(1, EMBEDDED_DB_NAME);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }
            if (!exists) {
                try (Statement st = c.createStatement()) {
                    st.execute("CREATE DATABASE " + EMBEDDED_DB_NAME);
                }
            }
        }
        return pg;
    }

    static JdbcTarget normalize(String url, String username, String password) {
        if (url.startsWith("jdbc:")) {
            return new JdbcTarget(url, username, password);
        }
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            URI uri = URI.create(url);
            String user = username;
            String pass = password;
            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                if (user == null || user.isBlank()) {
                    user = parts[0];
                }
                if ((pass == null || pass.isBlank()) && parts.length > 1) {
                    pass = parts[1];
                }
            }
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String query = uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "";
            return new JdbcTarget("jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query, user, pass);
        }
        throw new IllegalStateException("DB_URL debe empezar por jdbc:postgresql://, postgres:// o postgresql://");
    }

    record JdbcTarget(String jdbcUrl, String username, String password) {
    }
}
