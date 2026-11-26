package com.triviaquick.security;

import com.triviaquick.config.AppProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenResolver tokenResolver) throws Exception {
        http
                // CSRF: sustituido por CsrfHeaderFilter (API sin estado con cookie SameSite + cabecera obligatoria).
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> {
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(h -> h
                        .contentTypeOptions(c -> {
                        })
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                        // El token del enlace de verificación es la prueba; no requiere sesión.
                        .requestMatchers(HttpMethod.POST, "/api/profile/email/confirm").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/media", "/api/media/video").hasRole("TEACHER")
                        .requestMatchers("/api/teacher/**").hasRole("TEACHER")
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // El handshake WebSocket valida la identidad en WebSocketAuthInterceptor.
                        .requestMatchers("/ws", "/ws/**").permitAll()
                        // Roles comprobados en la cadena de filtros, antes de leer o validar el cuerpo.
                        // Los controladores repiten la regla con @PreAuthorize (defensa en profundidad).
                        .requestMatchers("/api/trivias", "/api/trivias/**").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/sessions",
                                "/api/sessions/*/start", "/api/sessions/*/advance", "/api/sessions/*/finish").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/sessions/join", "/api/sessions/*/answers",
                                "/api/sessions/*/events/*/responses", "/api/sessions/*/power-ups/*/activate").hasRole("STUDENT")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHENTICATED", "Inicia sesión para continuar."))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(res, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "No tienes permiso para realizar esta acción.")))
                .addFilterBefore(new JwtAuthenticationFilter(tokenResolver), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CsrfHeaderFilter(tokenResolver), JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.cors().allowedOrigins().stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", CsrfHeaderFilter.HEADER));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private void writeError(HttpServletResponse res, int status, String code, String message) throws IOException {
        JsonErrorWriter.write(res, status, code, message);
    }
}
