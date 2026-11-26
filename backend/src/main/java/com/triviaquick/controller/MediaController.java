package com.triviaquick.controller;

import com.triviaquick.exception.ApiException;
import com.triviaquick.service.MediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Imágenes y videos")
public class MediaController {

    private final MediaStorageService storage;

    public MediaController(MediaStorageService storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Subir una imagen para un cuestionario (portada o pregunta)",
            description = "PNG, JPG o GIF (y WebP/HEIC con Cloudinary), hasta 10 MB. Se valida por su contenido real.")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String key = storage.storeImage(file.getBytes(), MediaStorageService.Kind.CONTENT);
        return Map.of("key", key, "url", storage.url(key));
    }

    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Subir un video para una pregunta",
            description = "MP4 (y MOV/WebM con Cloudinary), hasta 50 MB. Se valida por su contenido real.")
    public Map<String, String> uploadVideo(@RequestParam("file") MultipartFile file) {
        String key = storage.storeVideo(file);
        return Map.of("key", key, "url", storage.url(key));
    }

    /** Sirve archivos del disco local (con soporte de Range para los videos). Con Cloudinary redirige a su CDN. */
    @GetMapping("/{key}")
    @Operation(summary = "Obtener una imagen o video almacenado")
    public ResponseEntity<Resource> get(@PathVariable String key) {
        if (!MediaStorageService.isValidKey(key)) {
            throw ApiException.notFound("El archivo no existe.");
        }
        if (storage.usesCloudinary()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(storage.url(key))).build();
        }
        Path file = storage.localFile(key).orElseThrow(() -> ApiException.notFound("El archivo no existe."));
        MediaType type = key.endsWith(".png") ? MediaType.IMAGE_PNG
                : key.endsWith(".mp4") ? MediaType.parseMediaType("video/mp4") : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate().immutable())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("Content-Security-Policy", "default-src 'none'")
                .body(new FileSystemResource(file));
    }
}
