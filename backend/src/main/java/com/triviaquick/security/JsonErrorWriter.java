package com.triviaquick.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Escribe errores JSON desde los filtros de seguridad (fuera de Spring MVC) con el mismo
 * formato que {@link com.triviaquick.exception.ApiError}.
 */
final class JsonErrorWriter {

    private JsonErrorWriter() {
    }

    static void write(HttpServletResponse res, int status, String code, String message) throws IOException {
        String json = "{\"status\":" + status
                + ",\"code\":\"" + escape(code)
                + "\",\"message\":\"" + escape(message)
                + "\",\"fieldErrors\":{},\"timestamp\":\"" + Instant.now() + "\"}";
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
