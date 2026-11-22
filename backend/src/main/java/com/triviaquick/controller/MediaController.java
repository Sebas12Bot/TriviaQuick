package com.triviaquick.controller;

import com.triviaquick.exception.ApiException;
import com.triviaquick.mapper.TriviaMapper;
import com.triviaquick.service.MediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@Tag(name = "Imágenes")
public class MediaController {

    private final MediaStorageService storage;

    public MediaController(MediaStorageService storage) {
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Subir una imagen para un cuestionario (portada o pregunta)",
            description = "PNG o JPG, validada por su contenido real y re-codificada en el servidor.")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String key = storage.storeImage(file.getBytes(), MediaStorageService.Kind.CONTENT);
        return Map.of("key", key, "url", TriviaMapper.mediaUrl(key));
    }

    @GetMapping("/{key}")
    @Operation(summary = "Obtener una imagen almacenada")
    public ResponseEntity<byte[]> get(@PathVariable String key) {
        MediaStorageService.StoredMedia media = storage.load(key)
                .orElseThrow(() -> ApiException.notFound("La imagen no existe."));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate().immutable())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("Content-Security-Policy", "default-src 'none'")
                .body(media.bytes());
    }
}
