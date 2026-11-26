package com.triviaquick.service;

import com.triviaquick.service.media.CloudinaryClient;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class MediaDetectionTest {

    private static byte[] ftyp(String brand) {
        byte[] b = new byte[32];
        System.arraycopy("ftyp".getBytes(StandardCharsets.US_ASCII), 0, b, 4, 4);
        System.arraycopy(brand.getBytes(StandardCharsets.US_ASCII), 0, b, 8, 4);
        return b;
    }

    @Test
    void detectsPhoneImageFormatsByContent() {
        byte[] webp = new byte[32];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, webp, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, webp, 8, 4);
        assertThat(MediaStorageService.detectImage(webp)).contains(MediaStorageService.ImageFormat.WEBP);
        assertThat(MediaStorageService.detectImage(ftyp("heic"))).contains(MediaStorageService.ImageFormat.HEIC);
        assertThat(MediaStorageService.detectImage(ftyp("mif1"))).contains(MediaStorageService.ImageFormat.HEIC);
        assertThat(MediaStorageService.detectImage("<html><script>".getBytes())).isEmpty();
    }

    @Test
    void detectsVideosAndDoesNotConfuseThemWithHeicPhotos() {
        assertThat(MediaStorageService.detectVideo(ftyp("isom"))).contains(MediaStorageService.VideoFormat.MP4);
        assertThat(MediaStorageService.detectVideo(ftyp("mp42"))).contains(MediaStorageService.VideoFormat.MP4);
        assertThat(MediaStorageService.detectVideo(ftyp("qt  "))).contains(MediaStorageService.VideoFormat.QUICKTIME);
        assertThat(MediaStorageService.detectVideo(new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0, 0}))
                .contains(MediaStorageService.VideoFormat.WEBM);
        assertThat(MediaStorageService.detectVideo(ftyp("heic"))).isEmpty();
        assertThat(MediaStorageService.detectVideo("PK\u0003\u0004 zip".getBytes())).isEmpty();
    }

    @Test
    void cloudinarySignatureMatchesTheDocumentedAlgorithm() throws Exception {
        // Ejemplo de la documentación de Cloudinary (parámetros ordenados + secreto, SHA-1).
        CloudinaryClient client = new CloudinaryClient("demo", "1234", "abcd", "f");
        Map<String, String> params = new TreeMap<>(Map.of(
                "eager", "w_400,h_300,c_pad|w_260,h_200,c_crop",
                "public_id", "sample_image",
                "timestamp", "1315060510"));
        Method sign = CloudinaryClient.class.getDeclaredMethod("sign", Map.class);
        sign.setAccessible(true);
        assertThat(sign.invoke(client, params)).isEqualTo("bfd09f95f331f558cbd1320e67aa8d488770583e");
    }

    @Test
    void cloudinaryUrlIsParsedAndBuildsDeliveryUrls() {
        CloudinaryClient c = CloudinaryClient.fromUrl("cloudinary://key:secret@micloud", "triviaquick").orElseThrow();
        assertThat(c.deliveryUrl(CloudinaryClient.ResourceType.VIDEO, "abc", "mp4"))
                .isEqualTo("https://res.cloudinary.com/micloud/video/upload/q_auto/triviaquick/abc.mp4");
        assertThat(CloudinaryClient.fromUrl("", "x")).isEmpty();
    }

    @Test
    void cloudinaryUrlAcceptsPastedLineAndNeverLeaksTheSecret() {
        CloudinaryClient pasted = CloudinaryClient.fromUrl("CLOUDINARY_URL=cloudinary://key:secret@micloud", "f").orElseThrow();
        assertThat(pasted.deliveryUrl(CloudinaryClient.ResourceType.IMAGE, "a", "jpg"))
                .isEqualTo("https://res.cloudinary.com/micloud/image/upload/q_auto/f/a.jpg");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> CloudinaryClient.fromUrl("cloud inary://key:topsecret@x", "f"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("topsecret");
    }
}
