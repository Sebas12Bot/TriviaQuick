package com.triviaquick.controller;

import com.triviaquick.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityIntegrationTest extends IntegrationTestBase {

    // 1. Un usuario no autenticado no puede consultar resultados privados.
    @Test
    void anonymousCannotReadResultsRankingOrState() throws Exception {
        mvc.perform(get("/api/sessions/1/results")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/sessions/1/ranking")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sessions/1/podium")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sessions/1/state")).andExpect(status().isUnauthorized());
    }

    @Test
    void forgedOrTamperedTokenIsRejected() throws Exception {
        TestUser s = student("Ana");
        String tampered = s.token().substring(0, s.token().length() - 3) + "abc";
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
    }

    // 2. Un estudiante no puede acceder a funciones de docente.
    @Test
    void studentCannotUseTeacherFeatures() throws Exception {
        TestUser teacher = teacher();
        TestUser student = student("Beto");
        TriviaInfo trivia = trivia(teacher, 2);
        SessionInfo s = session(teacher, trivia, false, 2);
        join(student, s);

        call(post("/api/trivias").content("{\"title\":\"x\",\"questions\":[]}"), student).andExpect(status().isForbidden());
        call(get("/api/trivias"), student).andExpect(status().isForbidden());
        call(post("/api/sessions").content("{\"triviaId\":" + trivia.id() + "}"), student).andExpect(status().isForbidden());
        start(student, s).andExpect(status().isForbidden());
        advance(student, s).andExpect(status().isForbidden());
        call(post("/api/sessions/" + s.id() + "/finish"), student).andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotControlAnotherTeachersSession() throws Exception {
        TestUser owner = teacher();
        TestUser other = teacher();
        SessionInfo s = session(owner, trivia(owner, 1), false, 2);
        start(other, s).andExpect(status().isForbidden());
        call(get("/api/sessions/" + s.id() + "/state"), other).andExpect(status().isForbidden());
    }

    @Test
    void teacherCannotAnswerAsParticipant() throws Exception {
        TestUser teacher = teacher();
        TriviaInfo t = trivia(teacher, 1);
        SessionInfo s = session(teacher, t, false, 2);
        answer(teacher, s, t.questions().get(0).id(), t.questions().get(0).correctAnswerId()).andExpect(status().isForbidden());
    }

    // 3. Un participante no puede consultar sesiones a las que no pertenece.
    @Test
    void nonMemberCannotReadForeignSession() throws Exception {
        TestUser teacher = teacher();
        TestUser member = student("Cris");
        TestUser outsider = student("Dani");
        TriviaInfo t = trivia(teacher, 1);
        SessionInfo s = session(teacher, t, false, 2);
        join(member, s);

        call(get("/api/sessions/" + s.id() + "/ranking"), outsider).andExpect(status().isForbidden());
        call(get("/api/sessions/" + s.id() + "/state"), outsider).andExpect(status().isForbidden());
        call(get("/api/sessions/" + s.id() + "/results"), outsider).andExpect(status().isForbidden());
        call(get("/api/sessions/" + s.id() + "/score-transactions"), outsider).andExpect(status().isForbidden());
        answer(outsider, s, t.questions().get(0).id(), t.questions().get(0).correctAnswerId()).andExpect(status().isForbidden());
        call(get("/api/sessions/" + s.id() + "/ranking"), member).andExpect(status().isOk());
    }

    @Test
    void stateNeverLeaksTheCorrectAnswerOrJoinCodeToParticipants() throws Exception {
        TestUser teacher = teacher();
        TestUser st = student("Eva");
        TriviaInfo t = trivia(teacher, 1);
        SessionInfo s = session(teacher, t, false, 2);
        join(st, s);
        start(teacher, s).andExpect(status().isOk());
        call(get("/api/sessions/" + s.id() + "/state"), st).andExpect(status().isOk())
                .andExpect(content().string(not(containsString("correct\":true"))))
                .andExpect(content().string(not(containsString("\"isCorrect\""))))
                .andExpect(jsonPath("$.joinCode").doesNotExist())
                .andExpect(jsonPath("$.currentQuestion.options[0].correct").doesNotExist());
    }

    @Test
    void teacherSignupRequiresServerCode() throws Exception {
        String body = """
                {"email":"nuevo-docente-%d@test.local","displayName":"Profe","password":"contrasena-segura","role":"TEACHER","teacherCode":"%s"}""";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(System.nanoTime(), "incorrecto")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(System.nanoTime(), "codigo-docente-pruebas")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(jsonPath("$.role").value("TEACHER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void cookieAuthenticatedWritesRequireAntiCsrfHeader() throws Exception {
        TestUser st = student("Fede");
        Cookie cookie = new Cookie("tq_session", st.token());
        mvc.perform(post("/api/sessions/join").cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ABCDEF\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_REJECTED"));
        mvc.perform(post("/api/sessions/join").cookie(cookie).header("X-Requested-With", "TriviaQuick")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"ABCDEF\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/auth/me").cookie(cookie)).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Fede"));
    }

    @Test
    void invalidInputIsRejectedWithoutInternalDetails() throws Exception {
        TestUser st = student("Gabi");
        call(post("/api/sessions/1/answers").content("{\"questionId\":-1}"), st)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.questionId").exists())
                .andExpect(content().string(not(containsString("Exception"))));
        call(post("/api/sessions/1/answers").content("{malformado"), st)
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("jackson"))));
    }

    @Test
    void corsOnlyAllowsConfiguredOrigins() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/login")
                        .header("Origin", "https://atacante.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
