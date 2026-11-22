package com.triviaquick.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Defensa CSRF para la autenticación por cookie: las peticiones que modifican estado deben incluir
 * la cabecera {@code X-Requested-With: TriviaQuick}. Un sitio ajeno no puede añadir cabeceras
 * personalizadas sin pasar la verificación CORS, que solo admite los orígenes autorizados.
 * Las peticiones autenticadas con cabecera Authorization (sin cookie) no son vulnerables a CSRF.
 */
public class CsrfHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Requested-With";
    public static final String EXPECTED = "TriviaQuick";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private final TokenResolver tokenResolver;

    public CsrfHeaderFilter(TokenResolver tokenResolver) {
        this.tokenResolver = tokenResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean unsafe = !SAFE_METHODS.contains(request.getMethod());
        boolean api = request.getRequestURI().startsWith("/api/");
        if (unsafe && api && tokenResolver.hasSessionCookie(request)
                && !EXPECTED.equals(request.getHeader(HEADER))) {
            JsonErrorWriter.write(response, HttpServletResponse.SC_FORBIDDEN, "CSRF_REJECTED",
                    "Solicitud rechazada por seguridad.");
            return;
        }
        chain.doFilter(request, response);
    }
}
