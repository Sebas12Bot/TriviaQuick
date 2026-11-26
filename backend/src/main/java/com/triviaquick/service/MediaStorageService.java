package com.triviaquick.service;

import com.triviaquick.config.AppProperties;
import com.triviaquick.exception.ApiException;
import com.triviaquick.service.media.CloudinaryClient;
import com.triviaquick.service.media.CloudinaryClient.ResourceType;
import com.triviaquick.service.media.MediaUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Almacenamiento de imágenes y videos. Con {@code CLOUDINARY_URL} se guardan en Cloudinary (permanente,
 * CDN, conversión de formatos de celular); sin él, en disco local (desarrollo).
 *
 * <p>Seguridad:
 * <ul>
 *   <li>El tipo se comprueba por los primeros bytes del archivo, no por la extensión ni el Content-Type.</li>
 *   <li>PNG, JPEG y GIF se decodifican y se vuelven a codificar en el servidor (sin metadatos ni contenido incrustado).</li>
 *   <li>HEIC/WebP (fotos de celular) solo se aceptan con Cloudinary, que las decodifica y convierte a JPG.</li>
 *   <li>El nombre lo genera el servidor (128 bits aleatorios); el cliente nunca elige rutas.</li>
 *   <li>Las claves se validan con un patrón estricto y la ruta local debe quedar dentro del directorio base.</li>
 * </ul>
 */
@Service
public class MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageService.class);

    public enum Kind { AVATAR, CONTENT }

    enum ImageFormat { PNG, JPEG, GIF, WEBP, HEIC }

    enum VideoFormat { MP4, QUICKTIME, WEBM }

    private static final Pattern KEY = Pattern.compile("^[a-f0-9]{32}\\.(png|jpg|mp4)$");
    private static final int MAX_DIMENSION = 8000;

    private final Path baseDir;
    private final long maxImageBytes;
    private final long maxVideoBytes;
    private final Optional<CloudinaryClient> cloudinary;
    private final SecureRandom random = new SecureRandom();

    public MediaStorageService(AppProperties props) throws IOException {
        AppProperties.Storage s = props.storage();
        this.baseDir = Path.of(s.dir()).toAbsolutePath().normalize();
        this.maxImageBytes = s.maxImageBytes();
        this.maxVideoBytes = s.maxVideoBytes();
        this.cloudinary = CloudinaryClient.fromUrl(s.cloudinaryUrl(), s.cloudinaryFolder());
        Files.createDirectories(baseDir);
        if (cloudinary.isPresent()) {
            CloudinaryClient c = cloudinary.get();
            MediaUrls.configure(key -> {
                String id = key.substring(0, key.indexOf('.'));
                String ext = key.substring(key.indexOf('.') + 1);
                return c.deliveryUrl("mp4".equals(ext) ? ResourceType.VIDEO : ResourceType.IMAGE, id, ext);
            });
            log.info("Almacenamiento de archivos: Cloudinary (carpeta {})", s.cloudinaryFolder());
        } else {
            MediaUrls.configure(key -> "/api/media/" + key);
            log.warn("CLOUDINARY_URL no está definido: los archivos se guardan en disco local ({}). "
                    + "En servidores como Render gratuito se pierden al reiniciar.", baseDir);
        }
    }

    public boolean usesCloudinary() {
        return cloudinary.isPresent();
    }

    // ------------------------------------------------------------------ imágenes

    public String storeImage(byte[] data, Kind kind) {
        if (data == null || data.length == 0) {
            throw ApiException.badRequest("INVALID_IMAGE", "No se recibió ninguna imagen.");
        }
        if (data.length > maxImageBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE",
                    "La imagen supera el tamaño máximo de " + (maxImageBytes / (1024 * 1024)) + " MB.");
        }
        ImageFormat format = detectImage(data).orElseThrow(() -> ApiException.badRequest("INVALID_IMAGE",
                "Formato no permitido. Usa una imagen PNG, JPG, GIF, WebP o HEIC."));
        String id = newId();

        if (format == ImageFormat.WEBP || format == ImageFormat.HEIC) {
            // Formatos de celular: Cloudinary los decodifica (y rechaza si no son imágenes reales) y los convierte a JPG.
            CloudinaryClient c = cloudinary.orElseThrow(() -> ApiException.badRequest("FORMAT_NEEDS_CLOUD",
                    "Este servidor solo admite PNG, JPG o GIF. Convierte la foto a JPG o configura Cloudinary."));
            String transformation = kind == Kind.AVATAR ? "c_fill,g_auto,w_256,h_256" : "c_limit,w_1600,h_1600";
            String ext = kind == Kind.AVATAR ? "png" : "jpg";
            uploadToCloud(c, ResourceType.IMAGE, id, () -> new ByteArrayInputStream(data), "foto",
                    Map.of("format", ext, "transformation", transformation), "INVALID_IMAGE",
                    "El archivo no es una imagen válida.");
            return id + "." + ext;
        }

        BufferedImage source = decode(data);
        try {
            byte[] encoded;
            String ext;
            if (kind == Kind.AVATAR) {
                encoded = encode(squareCrop(source, 256), "png");
                ext = "png";
            } else {
                encoded = encode(fitWidth(source, 1600), "jpg");
                ext = "jpg";
            }
            String key = id + "." + ext;
            if (cloudinary.isPresent()) {
                uploadToCloud(cloudinary.get(), ResourceType.IMAGE, id, () -> new ByteArrayInputStream(encoded), key,
                        Map.of(), "INVALID_IMAGE", "No se pudo guardar la imagen.");
            } else {
                Files.write(resolve(key), encoded);
            }
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    // ------------------------------------------------------------------ videos

    /** Guarda un video (MP4, MOV o WebM; MOV/WebM solo con Cloudinary, que los entrega como MP4). */
    public String storeVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("INVALID_VIDEO", "No se recibió ningún video.");
        }
        if (file.getSize() > maxVideoBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "VIDEO_TOO_LARGE",
                    "El video supera el tamaño máximo de " + (maxVideoBytes / (1024 * 1024)) + " MB.");
        }
        byte[] header = readHeader(file);
        VideoFormat format = detectVideo(header).orElseThrow(() -> ApiException.badRequest("INVALID_VIDEO",
                "Formato no permitido. Usa un video MP4, MOV o WebM."));
        String id = newId();
        String key = id + ".mp4";
        if (cloudinary.isPresent()) {
            uploadToCloud(cloudinary.get(), ResourceType.VIDEO, id, () -> {
                try {
                    return file.getInputStream();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, "video", Map.of(), "INVALID_VIDEO", "El archivo no es un video válido.");
            return key;
        }
        if (format != VideoFormat.MP4) {
            throw ApiException.badRequest("FORMAT_NEEDS_CLOUD",
                    "Este servidor solo admite videos MP4. Convierte el video o configura Cloudinary.");
        }
        try {
            file.transferTo(resolve(key));
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el video", e);
        }
    }

    // ------------------------------------------------------------------ lectura y borrado

    /** Archivo local de una clave (solo en modo disco). */
    public Optional<Path> localFile(String key) {
        if (!isValidKey(key) || cloudinary.isPresent()) {
            return Optional.empty();
        }
        Path path = resolve(key);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    public String url(String key) {
        return MediaUrls.of(key);
    }

    public void delete(String key) {
        if (!isValidKey(key)) {
            return;
        }
        String id = key.substring(0, 32);
        if (cloudinary.isPresent()) {
            cloudinary.get().destroy(key.endsWith(".mp4") ? ResourceType.VIDEO : ResourceType.IMAGE, id);
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

    // ------------------------------------------------------------------ detección y utilidades

    static Optional<ImageFormat> detectImage(byte[] d) {
        if (d.length > 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return Optional.of(ImageFormat.PNG);
        if (d.length > 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return Optional.of(ImageFormat.JPEG);
        if (d.length > 6 && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8') return Optional.of(ImageFormat.GIF);
        if (d.length > 12 && ascii(d, 0, 4).equals("RIFF") && ascii(d, 8, 4).equals("WEBP")) return Optional.of(ImageFormat.WEBP);
        if (d.length > 12 && ascii(d, 4, 4).equals("ftyp")) {
            String brand = ascii(d, 8, 4);
            if (brand.startsWith("hei") || brand.startsWith("hev") || brand.equals("mif1") || brand.equals("msf1")) {
                return Optional.of(ImageFormat.HEIC);
            }
        }
        return Optional.empty();
    }

    static Optional<VideoFormat> detectVideo(byte[] d) {
        if (d.length > 4 && (d[0] & 0xFF) == 0x1A && (d[1] & 0xFF) == 0x45 && (d[2] & 0xFF) == 0xDF && (d[3] & 0xFF) == 0xA3) {
            return Optional.of(VideoFormat.WEBM);
        }
        if (d.length > 12 && ascii(d, 4, 4).equals("ftyp")) {
            String brand = ascii(d, 8, 4);
            if (brand.startsWith("hei") || brand.startsWith("hev") || brand.equals("mif1") || brand.equals("msf1")) {
                return Optional.empty(); // es una foto HEIC, no un video
            }
            return Optional.of(brand.startsWith("qt") ? VideoFormat.QUICKTIME : VideoFormat.MP4);
        }
        return Optional.empty();
    }

    private static String ascii(byte[] d, int offset, int len) {
        return new String(d, offset, len, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static byte[] readHeader(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(32);
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_VIDEO", "No se pudo leer el archivo.");
        }
    }

    private static BufferedImage decode(byte[] data) {
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
        return source;
    }

    private void uploadToCloud(CloudinaryClient c, ResourceType type, String id, java.util.function.Supplier<InputStream> content,
                               String filename, Map<String, String> params, String invalidCode, String invalidMessage) {
        try {
            c.upload(type, id, content, filename, params);
        } catch (CloudinaryClient.UploadException e) {
            if (e.status() == 400) {
                throw ApiException.badRequest(invalidCode, invalidMessage);
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "STORAGE_UNAVAILABLE",
                    "El servicio de archivos no está disponible. Inténtalo de nuevo en unos minutos.");
        }
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
        double scale = Math.min(1.0, (double) maxWidth / Math.max(src.getWidth(), src.getHeight()));
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
