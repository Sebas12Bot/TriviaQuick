package com.triviaquick;

import com.jayway.jsonpath.JsonPath;
import com.triviaquick.model.Role;
import com.triviaquick.model.User;
import com.triviaquick.repository.UserRepository;
import com.triviaquick.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: contexto completo de Spring, PostgreSQL embebido real (Flyway incluido)
 * y MockMvc atravesando la cadena de seguridad. Cada prueba crea sus propios usuarios y sesiones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestBase.TestClockConfig.class)
public abstract class IntegrationTestBase {

    protected static final Instant T0 = Instant.parse("2026-01-15T10:00:00Z");
    private static final AtomicInteger SEQ = new AtomicInteger();

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(T0);
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected MutableClock clock;
    @Autowired protected UserRepository users;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected JwtService jwt;

    @BeforeEach
    void resetClock() {
        clock.set(T0);
    }

    public record TestUser(Long id, String token, String name) {
    }

    public record QuestionInfo(Long id, Long correctAnswerId, Long wrongAnswerId) {
    }

    public record TriviaInfo(Long id, List<QuestionInfo> questions) {
    }

    public record SessionInfo(Long id, String code) {
    }

    protected TestUser user(Role role, String name) {
        int n = SEQ.incrementAndGet();
        User u = users.save(new User("u" + n + "-" + System.nanoTime() + "@test.local", name,
                "{noop-not-used}" + n, role, T0));
        return new TestUser(u.getId(), jwt.issue(u.getId()), name);
    }

    protected TestUser teacher() {
        return user(Role.TEACHER, "Docente " + SEQ.get());
    }

    protected TestUser student(String name) {
        return user(Role.STUDENT, name);
    }

    protected MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req, TestUser u) {
        return req.header("Authorization", "Bearer " + u.token()).contentType(MediaType.APPLICATION_JSON);
    }

    protected ResultActions call(MockHttpServletRequestBuilder req, TestUser u) throws Exception {
        return mvc.perform(as(req, u));
    }

    protected static <T> T json(ResultActions r, String path) throws Exception {
        return JsonPath.read(r.andReturn().getResponse().getContentAsString(), path);
    }

    protected static Long longValue(Object o) {
        return ((Number) o).longValue();
    }

    /** Crea una trivia por API: cada pregunta tiene 20 s, 1000 puntos base y la primera opción correcta. */
    protected TriviaInfo trivia(TestUser teacher, int questions) throws Exception {
        StringBuilder qs = new StringBuilder();
        for (int i = 0; i < questions; i++) {
            if (i > 0) {
                qs.append(',');
            }
            qs.append("""
                    {"text":"Pregunta %d","timeLimitSeconds":20,"basePoints":1000,
                     "answers":[{"text":"Correcta","correct":true},{"text":"Incorrecta","correct":false},
                                {"text":"Otra","correct":false}]}""".formatted(i + 1));
        }
        ResultActions r = call(post("/api/trivias").content("""
                {"title":"Trivia de prueba","description":"x","questions":[%s]}""".formatted(qs)), teacher)
                .andExpect(status().isCreated());
        Long id = longValue(json(r, "$.id"));
        List<QuestionInfo> infos = new ArrayList<>();
        for (int i = 0; i < questions; i++) {
            infos.add(new QuestionInfo(longValue(json(r, "$.questions[" + i + "].id")),
                    longValue(json(r, "$.questions[" + i + "].answers[0].id")),
                    longValue(json(r, "$.questions[" + i + "].answers[1].id"))));
        }
        return new TriviaInfo(id, infos);
    }

    protected SessionInfo session(TestUser teacher, TriviaInfo trivia, boolean events, int frequency) throws Exception {
        ResultActions r = call(post("/api/sessions").content("""
                {"triviaId":%d,"eventsEnabled":%s,"eventFrequency":%d}""".formatted(trivia.id(), events, frequency)), teacher)
                .andExpect(status().isCreated());
        return new SessionInfo(longValue(json(r, "$.id")), json(r, "$.joinCode"));
    }

    protected void join(TestUser student, SessionInfo s) throws Exception {
        call(post("/api/sessions/join").content("{\"code\":\"" + s.code() + "\"}"), student).andExpect(status().isOk());
    }

    protected ResultActions start(TestUser teacher, SessionInfo s) throws Exception {
        return call(post("/api/sessions/" + s.id() + "/start"), teacher);
    }

    protected ResultActions advance(TestUser teacher, SessionInfo s) throws Exception {
        return call(post("/api/sessions/" + s.id() + "/advance"), teacher);
    }

    protected ResultActions answer(TestUser student, SessionInfo s, Long questionId, Long answerId) throws Exception {
        return call(post("/api/sessions/" + s.id() + "/answers")
                .content("{\"questionId\":" + questionId + ",\"answerId\":" + answerId + "}"), student);
    }

    protected int score(TestUser student, SessionInfo s) throws Exception {
        return json(call(get("/api/sessions/" + s.id() + "/state"), student).andExpect(status().isOk()), "$.me.score");
    }
}
