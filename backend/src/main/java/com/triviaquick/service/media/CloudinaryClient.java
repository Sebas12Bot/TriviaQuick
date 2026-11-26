package com.triviaquick.service.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Cliente mínimo de la API REST de Cloudinary (subidas firmadas y borrado), sin SDK.
 * Se configura con {@code CLOUDINARY_URL=cloudinary://API_KEY:API_SECRET@CLOUD_NAME}.
 * El secreto nunca sale del servidor: el navegador sube el archivo al backend y este a Cloudinary.
 */
public class CloudinaryClient {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryClient.class);

    public enum ResourceType { IMAGE, VIDEO }

    public static class UploadException extends RuntimeException {
        private final int status;

        public UploadException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int status() { return status; }
    }

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String folder;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public CloudinaryClient(String cloudName, String apiKey, String apiSecret, String folder) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.folder = folder;
    }

    /** Interpreta {@code cloudinary://key:secret@cloud}. Devuelve vacío si no está configurado. */
    public static Optional<CloudinaryClient> fromUrl(String url, String folder) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String value = url.trim();
        // Tolera que se pegue la línea completa del panel de Cloudinary ("CLOUDINARY_URL=cloudinary://...").
        if (value.regionMatches(true, 0, "CLOUDINARY_URL=", 0, 15)) {
            value = value.substring(15).trim();
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (java.net.URISyntaxException e) {
            uri = null;
        }
        if (uri == null || !"cloudinary".equals(uri.getScheme()) || uri.getUserInfo() == null
                || !uri.getUserInfo().contains(":") || uri.getHost() == null) {
            // Nunca se incluye el valor en el mensaje: contiene el API secret.
            throw new IllegalStateException("CLOUDINARY_URL debe tener el formato cloudinary://API_KEY:API_SECRET@CLOUD_NAME");
        }
        String[] creds = uri.getUserInfo().split(":", 2);
        return Optional.of(new CloudinaryClient(uri.getHost(), creds[0], creds[1], folder));
    }

    public String publicId(String id) {
        return folder + "/" + id;
    }

    /** URL de entrega. La extensión decide el formato que sirve Cloudinary (jpg, png o mp4). */
    public String deliveryUrl(ResourceType type, String id, String extension) {
        String transform = type == ResourceType.VIDEO ? "q_auto" : "q_auto";
        return "https://res.cloudinary.com/" + cloudName + "/" + type.name().toLowerCase(Locale.ROOT) + "/upload/"
                + transform + "/" + publicId(id) + "." + extension;
    }

    /**
     * Subida firmada. {@code extraParams} se incluyen en la firma (p. ej. {@code format} o {@code transformation}).
     */
    public void upload(ResourceType type, String id, Supplier<InputStream> content, String filename,
                       Map<String, String> extraParams) {
        Map<String, String> params = new TreeMap<>(extraParams);
        // La carpeta va dentro del public_id (y no en "folder"): así la URL de entrega coincide tanto en cuentas
        // con carpetas fijas como con carpetas dinámicas, donde "folder" no siempre forma parte del public_id.
        params.put("public_id", publicId(id));
        params.put("asset_folder", folder);
        params.put("overwrite", "false");
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("signature", sign(params));
        params.put("api_key", apiKey);

        String boundary = "----TriviaQuick" + UUID.randomUUID().toString().replace("-", "");
        List<HttpRequest.BodyPublisher> parts = new ArrayList<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            parts.add(HttpRequest.BodyPublishers.ofString("--" + boundary + "\r\nContent-Disposition: form-data; name=\""
                    + e.getKey() + "\"\r\n\r\n" + e.getValue() + "\r\n", StandardCharsets.UTF_8));
        }
        parts.add(HttpRequest.BodyPublishers.ofString("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + filename + "\"\r\nContent-Type: application/octet-stream\r\n\r\n", StandardCharsets.UTF_8));
        parts.add(HttpRequest.BodyPublishers.ofInputStream(content));
        parts.add(HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n", StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder(endpoint(type, "upload"))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.concat(parts.toArray(HttpRequest.BodyPublisher[]::new)))
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() / 100 != 2) {
            // Cloudinary responde 400 si el archivo no es una imagen/video válido.
            log.warn("Cloudinary rechazó la subida ({}): {}", response.statusCode(), abbreviate(response.body()));
            throw new UploadException(response.statusCode(), response.body());
        }
    }

    public void destroy(ResourceType type, String id) {
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId(id));
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("signature", sign(params));
        params.put("api_key", apiKey);
        String body = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(endpoint(type, "destroy"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            send(request);
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar {} de Cloudinary: {}", id, e.getMessage());
        }
    }

    /** Firma de Cloudinary: parámetros ordenados "k=v&k2=v2" + secreto, en SHA-1 hexadecimal. */
    String sign(Map<String, String> params) {
        String toSign = new TreeMap<>(params).entrySet().stream()
                .filter(e -> !List.of("file", "api_key", "resource_type", "signature").contains(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest((toSign + apiSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private URI endpoint(ResourceType type, String action) {
        return URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/" + type.name().toLowerCase(Locale.ROOT) + "/" + action);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UploadException(502, "No se pudo contactar con Cloudinary: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UploadException(503, "Subida interrumpida");
        }
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
