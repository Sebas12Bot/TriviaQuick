package com.triviaquick.service;

import com.triviaquick.config.AppProperties;
import com.triviaquick.exception.ApiException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Almacenamiento local de imágenes (avatares e imágenes de cuestionarios).
 *
 * <p>Medidas de seguridad:
 * <ul>
 *   <li>Se comprueba el tipo real por los primeros bytes (PNG o JPEG), no la extensión ni el Content-Type.</li>
 *   <li>La imagen se decodifica y se vuelve a codificar: se descartan metadatos y cualquier contenido incrustado.</li>
 *   <li>El nombre lo genera el servidor (128 bits aleatorios); el cliente nunca elige rutas.</li>
 *   <li>Las claves se validan con un patrón estricto y la ruta resuelta debe quedar dentro del directorio base.</li>
 * </ul>
 */
@Service
public class MediaStorageService {

    public enum Kind { AVATAR, CONTENT }

    public record StoredMedia(byte[] bytes, String contentType) {
    }

    private static final Pattern KEY = Pattern.compile("^[a-f0-9]{32}\\.(png|jpg)$");
    private static final int MAX_DIMENSION = 6000;

    private final Path baseDir;
    private final long maxBytes;
    private final SecureRandom random = new SecureRandom();

    public MediaStorageService(AppProperties props) throws IOException {
        this.baseDir = Path.of(props.storage().dir()).toAbsolutePath().normalize();
        this.maxBytes = props.storage().maxUploadBytes();
        Files.createDirectories(baseDir);
    }

    public String storeImage(byte[] data, Kind kind) {
        if (data == null || data.length == 0) {
            throw ApiException.badRequest("INVALID_IMAGE", "No se recibió ninguna imagen.");
        }
        if (data.length > maxBytes) {
            throw ApiException.badRequest("IMAGE_TOO_LARGE", "La imagen supera el tamaño máximo de " + (maxBytes / (1024 * 1024)) + " MB.");
        }
        if (!isPng(data) && !isJpeg(data)) {
            throw ApiException.badRequest("INVALID_IMAGE", "Formato no permitido. Usa una imagen PNG o JPG.");
        }
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException | RuntimeException e) {
            source = null;
        }
        if (source == null || source.getWidth() < 16 || source.getHeight() < 16
                || source.getWidth() > MAX_DIMENSION || source.getHeight() > MAX_DIMENSION) {
            throw ApiException.badRequest("INVALID_IMAGE", "El archivo no es una imagen válida o sus dimensiones no están permitidas.");
        }
        try {
            String key;
            byte[] encoded;
            if (kind == Kind.AVATAR) {
                encoded = encode(squareCrop(source, 256), "png");
                key = newId() + ".png";
            } else {
                encoded = encode(fitWidth(source, 1600), "jpg");
                key = newId() + ".jpg";
            }
            Files.write(resolve(key), encoded);
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la imagen", e);
        }
    }

    public Optional<StoredMedia> load(String key) {
        if (!isValidKey(key)) {
            return Optional.empty();
        }
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new StoredMedia(Files.readAllBytes(path), key.endsWith(".png") ? "image/png" : "image/jpeg"));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void delete(String key) {
        if (!isValidKey(key)) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ignored) {
            // Un archivo huérfano no afecta a la integridad de los datos.
        }
    }

    public static boolean isValidKey(String key) {
        return key != null && KEY.matcher(key).matches();
    }

    private Path resolve(String key) {
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir)) {
            throw ApiException.badRequest("INVALID_KEY", "Ruta no permitida.");
        }
        return path;
    }

    private String newId() {
        byte[] b = new byte[16];
        random.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }

    static boolean isPng(byte[] d) {
        return d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G';
    }

    static boolean isJpeg(byte[] d) {
        return d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF;
    }

    private static BufferedImage squareCrop(BufferedImage src, int size) {
        int side = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - side) / 2;
        int y = (src.getHeight() - side) / 2;
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, size, size, x, y, x + side, y + side, null);
        g.dispose();
        return out;
    }

    private static BufferedImage fitWidth(BufferedImage src, int maxWidth) {
        double scale = Math.min(1.0, (double) maxWidth / src.getWidth());
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static byte[] encode(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(img, format, out)) {
            throw new IOException("Formato no soportado: " + format);
        }
        return out.toByteArray();
    }
}
