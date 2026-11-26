package com.triviaquick.controller;

import com.triviaquick.IntegrationTestBase;
import com.triviaquick.model.Role;
import com.triviaquick.model.User;
import com.triviaquick.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProfileIntegrationTest extends IntegrationTestBase {

    private static final String PASSWORD = "contrasena-de-prueba";

    @Autowired ParticipantRepository participants;

    /** Usuario con contraseña real (el de la base usa un hash de relleno). */
    private TestUser withPassword(Role role, String name) {
        User u = users.save(new User(name.toLowerCase().replace(' ', '.') + System.nanoTime() + "@test.local", name,
                encoder.encode(PASSWORD), role, T0));
        return new TestUser(u.getId(), jwt.issue(u.getId()), name);
    }

    private static byte[] png(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void readAndRenameOwnProfile() throws Exception {
        TestUser u = withPassword(Role.STUDENT, "Ana Ruiz");
        call(get("/api/profile"), u).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ana Ruiz"))
                .andExpect(jsonPath("$.preferences.soundEnabled").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        call(patch("/api/profile").content("{\"displayName\":\"Ana R.\"}"), u).andExpect(jsonPath("$.displayName").value("Ana R."));
        call(patch("/api/profile").content("{\"displayName\":\"<script>\"}"), u).andExpect(status().isBadRequest());
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
    }

    @Test
    void preferencesPersistPerUser() throws Exception {
        TestUser a = withPassword(Role.STUDENT, "A");
        TestUser b = withPassword(Role.STUDENT, "B");
        call(put("/api/profile/preferences").content(
                "{\"soundEnabled\":false,\"masterVolume\":30,\"effectsVolume\":55,\"countdownSounds\":false}"), a)
                .andExpect(status().isOk());
        call(get("/api/profile"), a).andExpect(jsonPath("$.preferences.soundEnabled").value(false))
                .andExpect(jsonPath("$.preferences.masterVolume").value(30))
                .andExpect(jsonPath("$.preferences.effectsVolume").value(55));
        call(get("/api/profile"), b).andExpect(jsonPath("$.preferences.soundEnabled").value(true));
        call(put("/api/profile/preferences").content(
                "{\"soundEnabled\":true,\"masterVolume\":130,\"effectsVolume\":55,\"countdownSounds\":true}"), a)
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailChangeRequiresPasswordAndVerificationToken() throws Exception {
        TestUser u = withPassword(Role.STUDENT, "Beto");
        String newEmail = "nuevo" + System.nanoTime() + "@test.local";
        call(post("/api/profile/email").content("{\"newEmail\":\"" + newEmail + "\",\"currentPassword\":\"mala\"}"), u)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        call(post("/api/profile/email").content("{\"newEmail\":\"" + newEmail + "\",\"currentPassword\":\"" + PASSWORD + "\"}"), u)
                .andExpect(status().isOk()).andExpect(jsonPath("$.pendingEmail").value(newEmail))
                .andExpect(jsonPath("$.emailSent").value(false));
        // El correo no cambia hasta verificar.
        call(get("/api/profile"), u).andExpect(jsonPath("$.email").value(org.hamcrest.Matchers.not(newEmail)))
                .andExpect(jsonPath("$.pendingEmail").value(newEmail));
        mvc.perform(post("/api/profile/email/confirm").contentType("application/json")
                .content("{\"token\":\"token-falso-de-veinte-caracteres\"}")).andExpect(status().isBadRequest());
        // Simula el enlace: genera un token conocido y guarda su hash.
        byte[] raw = new byte[32];
        new java.security.SecureRandom().nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        User user = users.findById(u.id()).orElseThrow();
        user.requestEmailChange(newEmail, sha256(token), clock.instant().plusSeconds(600));
        users.save(user);
        mvc.perform(post("/api/profile/email/confirm").contentType("application/json").content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isNoContent());
        call(get("/api/profile"), u).andExpect(jsonPath("$.email").value(newEmail)).andExpect(jsonPath("$.pendingEmail").doesNotExist());
        // El token no se puede reutilizar.
        mvc.perform(post("/api/profile/email/confirm").contentType("application/json").content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredEmailTokenIsRejected() throws Exception {
        TestUser u = withPassword(Role.STUDENT, "Cris");
        String token = "token-caducado-" + System.nanoTime();
        User user = users.findById(u.id()).orElseThrow();
        user.requestEmailChange("caducado" + System.nanoTime() + "@test.local", sha256(token), clock.instant().minusSeconds(1));
        users.save(user);
        mvc.perform(post("/api/profile/email/confirm").contentType("application/json").content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void avatarUploadValidatesRealContent() throws Exception {
        TestUser u = withPassword(Role.STUDENT, "Dani");
        ResultActions ok = mvc.perform(multipart("/api/profile/avatar")
                        .file(new MockMultipartFile("file", "foto.png", "image/png", png(400, 300)))
                        .header("Authorization", "Bearer " + u.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.avatarUrl").exists());
        String url = json(ok, "$.avatarUrl");
        call(get(url), u).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        // Un HTML disfrazado de PNG se rechaza.
        mvc.perform(multipart("/api/profile/avatar")
                        .file(new MockMultipartFile("file", "x.png", "image/png", "<html><script>alert(1)</script>".getBytes()))
                        .header("Authorization", "Bearer " + u.token()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        // Una cabecera PNG con contenido corrupto también.
        byte[] fake = png(40, 40);
        java.util.Arrays.fill(fake, 16, fake.length, (byte) 7);
        mvc.perform(multipart("/api/profile/avatar").file(new MockMultipartFile("file", "x.png", "image/png", fake))
                        .header("Authorization", "Bearer " + u.token()))
                .andExpect(status().isBadRequest());
        // Rutas manipuladas no salen del almacén.
        call(get("/api/media/..%2F..%2Fetc%2Fpasswd"), u).andExpect(status().is4xxClientError());
        call(delete("/api/profile/avatar"), u).andExpect(jsonPath("$.avatarUrl").doesNotExist());
        call(get(url), u).andExpect(status().isNotFound());
    }

    @Test
    void localStorageAcceptsMp4VideoAndExplainsWhatNeedsCloudinary() throws Exception {
        TestUser t = withPassword(Role.TEACHER, "Profe Video");
        byte[] mp4 = new byte[64];
        System.arraycopy("ftypisom".getBytes(), 0, mp4, 4, 8);
        ResultActions ok = mvc.perform(multipart("/api/media/video").file(new MockMultipartFile("file", "clip.mp4", "video/mp4", mp4))
                        .header("Authorization", "Bearer " + t.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(org.hamcrest.Matchers.matchesPattern("^[a-f0-9]{32}\\.mp4$")));
        call(get((String) json(ok, "$.url")), t).andExpect(status().isOk()).andExpect(content().contentType("video/mp4"));
        // MOV y fotos HEIC/WebP requieren Cloudinary: sin él, el error lo explica.
        byte[] mov = new byte[64];
        System.arraycopy("ftypqt  ".getBytes(), 0, mov, 4, 8);
        mvc.perform(multipart("/api/media/video").file(new MockMultipartFile("file", "clip.mov", "video/quicktime", mov))
                .header("Authorization", "Bearer " + t.token())).andExpect(jsonPath("$.code").value("FORMAT_NEEDS_CLOUD"));
        byte[] heic = new byte[64];
        System.arraycopy("ftypheic".getBytes(), 0, heic, 4, 8);
        mvc.perform(multipart("/api/media").file(new MockMultipartFile("file", "foto.heic", "image/heic", heic))
                .header("Authorization", "Bearer " + t.token())).andExpect(jsonPath("$.code").value("FORMAT_NEEDS_CLOUD"));
        // Un ejecutable disfrazado de video se rechaza.
        mvc.perform(multipart("/api/media/video").file(new MockMultipartFile("file", "x.mp4", "video/mp4", "MZ\u0090\u0000 malware".getBytes()))
                .header("Authorization", "Bearer " + t.token())).andExpect(jsonPath("$.code").value("INVALID_VIDEO"));
        // Estudiantes no suben videos.
        TestUser s = withPassword(Role.STUDENT, "Alumno");
        mvc.perform(multipart("/api/media/video").file(new MockMultipartFile("file", "clip.mp4", "video/mp4", mp4))
                .header("Authorization", "Bearer " + s.token())).andExpect(status().isForbidden());
    }

    @Test
    void studentsCannotUploadQuizImages() throws Exception {
        TestUser s = withPassword(Role.STUDENT, "Eva");
        mvc.perform(multipart("/api/media").file(new MockMultipartFile("file", "a.png", "image/png", png(50, 50)))
                .header("Authorization", "Bearer " + s.token())).andExpect(status().isForbidden());
        TestUser t = withPassword(Role.TEACHER, "Profe");
        mvc.perform(multipart("/api/media").file(new MockMultipartFile("file", "a.png", "image/png", png(50, 50)))
                .header("Authorization", "Bearer " + t.token())).andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(org.hamcrest.Matchers.matchesPattern("^[a-f0-9]{32}\\.jpg$")));
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndInvalidatesOldSessions() throws Exception {
        TestUser u = withPassword(Role.STUDENT, "Fede");
        call(post("/api/profile/password").content(
                "{\"currentPassword\":\"mala\",\"newPassword\":\"otra-contrasena-1\",\"confirmPassword\":\"otra-contrasena-1\"}"), u)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        call(post("/api/profile/password").content(
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"otra-contrasena-1\",\"confirmPassword\":\"distinta-1234\"}"), u)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
        clock.advance(java.time.Duration.ofSeconds(5));
        ResultActions r = call(post("/api/profile/password").content(
                "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"otra-contrasena-1\",\"confirmPassword\":\"otra-contrasena-1\"}"), u)
                .andExpect(status().isOk()).andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")));
        assertThat(users.findById(u.id()).orElseThrow().getPasswordHash()).doesNotContain("otra-contrasena-1");
        // El token anterior ya no sirve; el de la cookie nueva sí.
        call(get("/api/profile"), u).andExpect(status().isUnauthorized());
        Matcher m = Pattern.compile("tq_session=([^;]+)").matcher(r.andReturn().getResponse().getHeader("Set-Cookie"));
        assertThat(m.find()).isTrue();
        mvc.perform(get("/api/profile").header("Authorization", "Bearer " + m.group(1))).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType("application/json").content(
                "{\"email\":\"" + users.findById(u.id()).orElseThrow().getEmail() + "\",\"password\":\"otra-contrasena-1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deletingAccountAnonymizesButKeepsOthersHistory() throws Exception {
        TestUser teacher = teacher();
        TestUser leaving = withPassword(Role.STUDENT, "Gabi Sale");
        TestUser staying = withPassword(Role.STUDENT, "Hugo Queda");
        TriviaInfo t = trivia(teacher, 1);
        SessionInfo s = session(teacher, t, false, 2);
        join(leaving, s);
        join(staying, s);
        start(teacher, s);
        answer(leaving, s, t.questions().get(0).id(), t.questions().get(0).correctAnswerId());
        answer(staying, s, t.questions().get(0).id(), t.questions().get(0).wrongAnswerId());
        advance(teacher, s).andExpect(jsonPath("$.status").value("FINISHED"));

        call(delete("/api/profile").content("{\"currentPassword\":\"" + PASSWORD + "\",\"confirmation\":\"borrar\"}"), leaving)
                .andExpect(status().isBadRequest());
        call(delete("/api/profile").content("{\"currentPassword\":\"mala\",\"confirmation\":\"ELIMINAR\"}"), leaving)
                .andExpect(status().isBadRequest());
        call(delete("/api/profile").content("{\"currentPassword\":\"" + PASSWORD + "\",\"confirmation\":\"ELIMINAR\"}"), leaving)
                .andExpect(status().isNoContent());

        User gone = users.findById(leaving.id()).orElseThrow();
        assertThat(gone.isActive()).isFalse();
        assertThat(gone.getEmail()).endsWith("@cuenta-eliminada.invalid");
        assertThat(gone.getDisplayName()).isEqualTo("Cuenta eliminada");
        call(get("/api/profile"), leaving).andExpect(status().isUnauthorized());
        // El resultado de la partida se conserva para los demás, con el nombre anonimizado.
        call(get("/api/sessions/" + s.id() + "/results"), staying).andExpect(status().isOk())
                .andExpect(jsonPath("$.ranking.length()").value(2))
                .andExpect(jsonPath("$.ranking[0].displayName").value("Participante eliminado"))
                .andExpect(jsonPath("$.ranking[0].score").value(1200)) // incluye el bonus de la más rápida
                .andExpect(jsonPath("$.ranking[1].displayName").value("Hugo Queda"));
    }

    private static String sha256(String v) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(v.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
