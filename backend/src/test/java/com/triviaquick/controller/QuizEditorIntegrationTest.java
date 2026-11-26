package com.triviaquick.controller;

import com.triviaquick.IntegrationTestBase;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Editor de cuestionarios: borradores, publicación, línea de tiempo, modos de eventos y puntuación configurable. */
class QuizEditorIntegrationTest extends IntegrationTestBase {

    private static String question(String text, int points, boolean firstCorrect) {
        return """
                {"type":"MULTIPLE_CHOICE","text":"%s","timeLimitSeconds":20,"basePoints":%d,
                 "answers":[{"text":"A","correct":%s},{"text":"B","correct":false},{"text":"C","correct":false}]}"""
                .formatted(text, points, firstCorrect);
    }

    private static String quiz(String status, String questions, String extra) {
        return """
                {"title":"Mi cuestionario","description":"d","category":"Ciencias","status":"%s","playMode":"GROUP",
                 "questions":[%s]%s}""".formatted(status, questions, extra == null ? "" : "," + extra);
    }

    private Long create(TestUser teacher, String body) throws Exception {
        return longValue(json(call(post("/api/trivias").content(body), teacher).andExpect(status().isCreated()), "$.id"));
    }

    @Test
    void draftCanBeIncompleteButPublishingRequiresAValidQuiz() throws Exception {
        TestUser teacher = teacher();
        // Borrador con una pregunta sin respuesta correcta: se guarda.
        Long id = create(teacher, quiz("DRAFT", question("Incompleta", 100, false), null));
        call(get("/api/trivias/" + id), teacher).andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.category").value("Ciencias"));
        // Publicar así falla.
        call(put("/api/trivias/" + id).content(quiz("PUBLISHED", question("Incompleta", 100, false), null)), teacher)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_QUESTION"));
        // Un borrador no se puede jugar.
        call(post("/api/sessions").content("{\"triviaId\":" + id + "}"), teacher)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIVIA_DRAFT"));
        // Corregido y publicado.
        call(put("/api/trivias/" + id).content(quiz("PUBLISHED", question("Lista", 100, true), null)), teacher)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        call(post("/api/sessions").content("{\"triviaId\":" + id + "}"), teacher).andExpect(status().isCreated());
    }

    @Test
    void editingReordersQuestionsAndKeepsPerQuestionPoints() throws Exception {
        TestUser teacher = teacher();
        Long id = create(teacher, quiz("DRAFT", question("Uno", 100, true) + "," + question("Dos", 200, true)
                + "," + question("Tres", 300, true), null));
        ResultActions r = call(put("/api/trivias/" + id).content(quiz("DRAFT",
                question("Tres", 300, true) + "," + question("Uno", 100, true) + "," + question("Dos editada", 250, true), null)), teacher)
                .andExpect(status().isOk());
        assertThat((JSONArray) json(r, "$.questions[*].text")).containsExactly("Tres", "Uno", "Dos editada");
        assertThat((JSONArray) json(r, "$.questions[*].basePoints")).containsExactly(300, 100, 250);
        assertThat((JSONArray) json(r, "$.questions[*].position")).containsExactly(0, 1, 2);
    }

    @Test
    void anotherTeacherCannotReadOrModifyAQuiz() throws Exception {
        TestUser owner = teacher();
        TestUser other = teacher();
        TestUser student = student("Eva");
        Long id = create(owner, quiz("DRAFT", question("P", 100, true), null));
        call(get("/api/trivias/" + id), other).andExpect(status().isForbidden());
        call(put("/api/trivias/" + id).content(quiz("DRAFT", question("Hackeada", 100, true), null)), other)
                .andExpect(status().isForbidden());
        call(delete("/api/trivias/" + id), other).andExpect(status().isForbidden());
        call(put("/api/trivias/" + id).content(quiz("DRAFT", question("Hackeada", 100, true), null)), student)
                .andExpect(status().isForbidden());
        call(get("/api/trivias/" + id), owner).andExpect(jsonPath("$.questions[0].text").value("P"));
    }

    @Test
    void manualTimelineValidatesSlotsAndRunsExactlyTheConfiguredEvents() throws Exception {
        TestUser teacher = teacher();
        String qs = question("1", 100, true) + "," + question("2", 100, true) + "," + question("3", 100, true);
        // No se permite un evento después de la última pregunta.
        call(post("/api/trivias").content(quiz("PUBLISHED", qs,
                "\"events\":{\"mode\":\"MANUAL\"},\"timeline\":[{\"afterQuestion\":2,\"type\":\"DUEL\"}]")), teacher)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_TIMELINE"));
        // Ni dos eventos en el mismo hueco.
        call(post("/api/trivias").content(quiz("PUBLISHED", qs,
                "\"events\":{\"mode\":\"MANUAL\"},\"timeline\":[{\"afterQuestion\":0,\"type\":\"DUEL\"},{\"afterQuestion\":0,\"type\":\"SHIELD\"}]")), teacher)
                .andExpect(status().isBadRequest());

        // Un multiplicador x3 tras la pregunta 1 (índice 0), para todos.
        Long id = create(teacher, quiz("PUBLISHED", qs, "\"events\":{\"mode\":\"MANUAL\"},"
                + "\"timeline\":[{\"afterQuestion\":0,\"type\":\"MULTIPLIER\",\"multiplierFactor\":3,\"target\":\"ALL\"}]"));
        call(get("/api/trivias/" + id), teacher).andExpect(jsonPath("$.events.mode").value("MANUAL"))
                .andExpect(jsonPath("$.timeline[0].type").value("MULTIPLIER"))
                .andExpect(jsonPath("$.timeline[0].multiplierFactor").value(3));

        TestUser ana = student("Ana");
        ResultActions detail = call(get("/api/trivias/" + id), teacher);
        Long q1 = longValue(json(detail, "$.questions[0].id"));
        Long q1ok = longValue(json(detail, "$.questions[0].answers[0].id"));
        Long q2 = longValue(json(detail, "$.questions[1].id"));
        Long q2ok = longValue(json(detail, "$.questions[1].answers[0].id"));
        ResultActions created = call(post("/api/sessions").content("{\"triviaId\":" + id + "}"), teacher).andExpect(status().isCreated());
        SessionInfo s = new SessionInfo(longValue(json(created, "$.id")), json(created, "$.joinCode"));
        join(ana, s);
        start(teacher, s);
        answer(ana, s, q1, q1ok).andExpect(status().isOk());
        advance(teacher, s).andExpect(jsonPath("$.status").value("EVENT"))
                .andExpect(jsonPath("$.currentEvent.type").value("MULTIPLIER"));
        ResultActions st = call(get("/api/sessions/" + s.id() + "/state"), ana).andExpect(jsonPath("$.myPowerUps[0].type").value("MULTIPLIER"));
        Long pu = longValue(json(st, "$.myPowerUps[0].id"));
        call(post("/api/sessions/" + s.id() + "/power-ups/" + pu + "/activate"), ana).andExpect(status().isOk());
        advance(teacher, s).andExpect(jsonPath("$.status").value("QUESTION"));
        answer(ana, s, q2, q2ok).andExpect(jsonPath("$.multiplierFactor").value(3))
                .andExpect(jsonPath("$.pointsAwarded").value((100 + 100) * 3));
        // Tras la pregunta 2 no hay evento configurado: pasa directamente a la 3.
        advance(teacher, s).andExpect(jsonPath("$.status").value("QUESTION"))
                .andExpect(jsonPath("$.currentQuestion.index").value(2));
    }

    @Test
    void autoModeOnlyUsesEnabledTypesWithoutConsecutiveRepeats() throws Exception {
        TestUser teacher = teacher();
        StringBuilder qs = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            qs.append(i > 0 ? "," : "").append(question("Q" + i, 100, true));
        }
        Long id = create(teacher, quiz("PUBLISHED", qs.toString(),
                "\"events\":{\"mode\":\"AUTO\",\"autoFrequency\":1,\"autoTypes\":[\"LIGHTNING_CHALLENGE\",\"SHIELD\"]}"));
        TestUser ana = student("Ana");
        ResultActions created = call(post("/api/sessions").content("{\"triviaId\":" + id + "}"), teacher).andExpect(status().isCreated());
        SessionInfo s = new SessionInfo(longValue(json(created, "$.id")), json(created, "$.joinCode"));
        join(ana, s);
        start(teacher, s);
        String previous = null;
        for (int i = 0; i < 4; i++) {
            ResultActions ev = advance(teacher, s).andExpect(jsonPath("$.status").value("EVENT"));
            String type = json(ev, "$.currentEvent.type");
            assertThat(type).isIn("LIGHTNING_CHALLENGE", "SHIELD").isNotEqualTo(previous);
            previous = type;
            advance(teacher, s).andExpect(jsonPath("$.status").value("QUESTION"));
        }
        advance(teacher, s).andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void autoModeRequiresAtLeastOneEventType() throws Exception {
        TestUser teacher = teacher();
        call(post("/api/trivias").content(quiz("PUBLISHED", question("1", 100, true),
                "\"events\":{\"mode\":\"AUTO\",\"autoTypes\":[]}")), teacher)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EVENTS"));
    }

    @Test
    void configurableSpeedAndStreakBonusesAreAppliedByTheServer() throws Exception {
        TestUser teacher = teacher();
        StringBuilder qs = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            qs.append(i > 0 ? "," : "").append(question("Q" + i, 200, true));
        }
        String scoring = "\"scoring\":{\"speedBonusEnabled\":true,\"speedTiers\":[{\"maxElapsedPercent\":50,\"bonusPoints\":80},"
                + "{\"maxElapsedPercent\":100,\"bonusPoints\":20}],\"streakEnabled\":true,\"streakBreaksOnMiss\":true,"
                + "\"streakTiers\":[{\"streakLength\":2,\"bonusPoints\":40}],\"fastestBonusEnabled\":false},\"events\":{\"mode\":\"OFF\"}";
        Long id = create(teacher, quiz("PUBLISHED", qs.toString(), scoring));
        ResultActions detail = call(get("/api/trivias/" + id), teacher);
        TestUser ana = student("Ana");
        ResultActions created = call(post("/api/sessions").content("{\"triviaId\":" + id + "}"), teacher).andExpect(status().isCreated());
        SessionInfo s = new SessionInfo(longValue(json(created, "$.id")), json(created, "$.joinCode"));
        join(ana, s);
        start(teacher, s);

        // P1 rápida: 200 + 80. Racha 1.
        answer(ana, s, longValue(json(detail, "$.questions[0].id")), longValue(json(detail, "$.questions[0].answers[0].id")))
                .andExpect(jsonPath("$.pointsAwarded").value(280)).andExpect(jsonPath("$.streak").value(1))
                .andExpect(jsonPath("$.streakBonus").value(0));
        advance(teacher, s);
        // P2 lenta (15 s de 20 → 75 %): 200 + 20; racha 2 → +40.
        clock.advance(Duration.ofSeconds(15));
        answer(ana, s, longValue(json(detail, "$.questions[1].id")), longValue(json(detail, "$.questions[1].answers[0].id")))
                .andExpect(jsonPath("$.speedBonus").value(20)).andExpect(jsonPath("$.streak").value(2))
                .andExpect(jsonPath("$.streakBonus").value(40)).andExpect(jsonPath("$.totalScore").value(280 + 220 + 40));
        advance(teacher, s);
        // P3 incorrecta: rompe la racha.
        answer(ana, s, longValue(json(detail, "$.questions[2].id")), longValue(json(detail, "$.questions[2].answers[1].id")))
                .andExpect(jsonPath("$.streak").value(0)).andExpect(jsonPath("$.pointsAwarded").value(0));
        advance(teacher, s);
        answer(ana, s, longValue(json(detail, "$.questions[3].id")), longValue(json(detail, "$.questions[3].answers[0].id")))
                .andExpect(jsonPath("$.streak").value(1)).andExpect(jsonPath("$.streakBonus").value(0));
        // Historial: incluye el movimiento de racha.
        call(get("/api/sessions/" + s.id() + "/score-transactions"), ana)
                .andExpect(jsonPath("$[?(@.reason == 'STREAK_BONUS')].delta").value(org.hamcrest.Matchers.contains(40)));
    }

    @Test
    void invalidScoringIsRejected() throws Exception {
        TestUser teacher = teacher();
        call(post("/api/trivias").content(quiz("DRAFT", question("1", 100, true),
                "\"scoring\":{\"speedTiers\":[{\"maxElapsedPercent\":30,\"bonusPoints\":10},{\"maxElapsedPercent\":80,\"bonusPoints\":90}]}")), teacher)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SCORING"));
        // Una bonificación activada sin tramos no tendría efecto: se rechaza.
        call(post("/api/trivias").content(quiz("DRAFT", question("1", 100, true),
                "\"scoring\":{\"speedBonusEnabled\":true,\"speedTiers\":[]}")), teacher)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_SCORING"));
        // Desactivada, sin tramos, sí es válida.
        call(post("/api/trivias").content(quiz("DRAFT", question("1", 100, true),
                "\"scoring\":{\"speedBonusEnabled\":false,\"speedTiers\":[],\"streakEnabled\":false,\"streakTiers\":[]}")), teacher)
                .andExpect(status().isCreated());
    }

    @Test
    void playedQuizLocksQuestionsButCanBeDuplicated() throws Exception {
        TestUser teacher = teacher();
        Long id = create(teacher, quiz("PUBLISHED", question("Original", 100, true), null));
        ResultActions created = call(post("/api/sessions").content("{\"triviaId\":" + id + "}"), teacher).andExpect(status().isCreated());
        Long sessionId = longValue(json(created, "$.id"));
        // Con una partida en curso no se puede editar nada.
        call(put("/api/trivias/" + id).content(quiz("PUBLISHED", question("Original", 100, true), null)), teacher)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRIVIA_IN_USE"));
        call(post("/api/sessions/" + sessionId + "/finish"), teacher).andExpect(status().isOk());
        // Terminada: los metadatos sí; las preguntas no.
        call(put("/api/trivias/" + id).content(quiz("PUBLISHED", question("Original", 100, true), null).replace("Mi cuestionario", "Renombrado")), teacher)
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Renombrado"))
                .andExpect(jsonPath("$.questionsLocked").value(true));
        call(put("/api/trivias/" + id).content(quiz("PUBLISHED", question("Cambiada", 100, true), null)), teacher)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("QUESTIONS_LOCKED"));
        call(delete("/api/trivias/" + id), teacher).andExpect(status().isConflict());
        ResultActions copy = call(post("/api/trivias/" + id + "/duplicate"), teacher).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.questionsLocked").value(false));
        Long copyId = longValue(json(copy, "$.id"));
        call(put("/api/trivias/" + copyId).content(quiz("DRAFT", question("Cambiada", 100, true), null)), teacher)
                .andExpect(status().isOk());
        call(delete("/api/trivias/" + copyId), teacher).andExpect(status().isNoContent());
    }

    @Test
    void dashboardReportsRealCounts() throws Exception {
        TestUser teacher = teacher();
        create(teacher, quiz("DRAFT", question("1", 100, true), null));
        Long pub = create(teacher, quiz("PUBLISHED", question("1", 100, true), null));
        call(get("/api/teacher/dashboard"), teacher).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuizzes").value(2))
                .andExpect(jsonPath("$.publishedQuizzes").value(1))
                .andExpect(jsonPath("$.draftQuizzes").value(1))
                .andExpect(jsonPath("$.finishedSessions").value(0))
                .andExpect(jsonPath("$.recentResults.length()").value(0));
        ResultActions created = call(post("/api/sessions").content("{\"triviaId\":" + pub + "}"), teacher);
        call(post("/api/sessions/" + longValue(json(created, "$.id")) + "/finish"), teacher);
        call(get("/api/teacher/dashboard"), teacher).andExpect(jsonPath("$.finishedSessions").value(1))
                .andExpect(jsonPath("$.recentResults[0].participantCount").value(0));
        call(get("/api/teacher/dashboard"), student("X")).andExpect(status().isForbidden());
    }
}
