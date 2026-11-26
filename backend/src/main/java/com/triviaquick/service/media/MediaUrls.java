package com.triviaquick.service.media;

import java.util.function.Function;

/**
 * Traduce una clave de archivo a su URL pública. Lo configura {@code MediaStorageService} al arrancar:
 * con Cloudinary apunta a su CDN; sin él, al endpoint local {@code /api/media/{clave}}.
 */
public final class MediaUrls {

    private static volatile Function<String, String> resolver = key -> "/api/media/" + key;

    private MediaUrls() {
    }

    public static void configure(Function<String, String> newResolver) {
        resolver = newResolver;
    }

    public static String of(String key) {
        return key == null ? null : resolver.apply(key);
    }
}
