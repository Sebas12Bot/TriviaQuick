package com.triviaquick.controller;

import com.triviaquick.IntegrationTestBase;
import com.triviaquick.repository.ParticipantRepository;
import com.triviaquick.repository.ScoreTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bonus para la respuesta correcta más rápida y reporte individual para el docente. */
class FastestAndReportIntegrationTest extends IntegrationTestBase {

    @Autowired ParticipantRepository participants;
    @Autowired ScoreTransactionRepository transactions;

    private long fastestBonuses(SessionInfo s, TestUser u) {
        Long pid = participants.findBySessionIdAndUserId(s.id(), u.id()).orElseThrow().getId();
        return transactions.findByParticipantIdOrderByCreatedAtAscIdAsc(pid).stream()
                .filter(t -> t.getReason().name().equals("FASTEST_ANSWER")).count();
    }

    @Test
    void fastestCorrectAnswerGetsTheBonusWhenTheQuestionCloses() throws Exception {
        TestUser teacher = teacher();
        TestUser lento = student("Lento");
        TestUser rapido = student("Rápido");
        TestUser fallo = student("Fallo");
        TriviaInfo t = trivia(teacher, 2);
        SessionInfo s = session(teacher, t, false, 2);
        join(lento, s);
        join(rapido, s);
        join(fallo, s);
        start(teacher, s);
        QuestionInfo q = t.questions().get(0);
        // El que falla responde primero: una respuesta incorrecta nunca gana el bonus.
        answer(fallo, s, q.id(), q.wrongAnswerId());
        clock.advance(Duration.ofMillis(1500));
        answer(rapido, s, q.id(), q.correctAnswerId());
        clock.advance(Duration.ofMillis(800));
        answer(lento, s, q.id(), q.correctAnswerId());
        int before = score(rapido, s);
        // Durante la pregunta nadie tiene el bonus todavía (no se revela nada).
        assertThat(fastestBonuses(s, rapido)).isZero();

        advance(teacher, s).andExpect(status().isOk());
        assertThat(score(rapido, s)).isEqualTo(before + 100);
        assertThat(fastestBonuses(s, rapido)).isEqualTo(1);
        assertThat(fastestBonuses(s, lento)).isZero();
        assertThat(fastestBonuses(s, fallo)).isZero();

        // Cerrar la partida sin respuestas en la 2.ª no concede nada más ni repite el de la 1.ª.
        call(post("/api/sessions/" + s.id() + "/finish"), teacher).andExpect(status().isOk());
        assertThat(fastestBonuses(s, rapido)).isEqualTo(1);
    }

    @Test
    void fastestBonusCanBeDisabledAndConfigured() throws Exception {
        TestUser teacher = teacher();
        String q = """
                {"text":"P","timeLimitSeconds":20,"basePoints":100,"answers":[{"text":"A","correct":true},{"text":"B","correct":false}]}""";
        ResultActions off = call(post("/api/trivias").content("""
                {"title":"Sin bonus","questions":[%s],"scoring":{"fastestBonusEnabled":false},"events":{"mode":"OFF"}}""".formatted(q)), teacher)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.scoring.fastestBonusEnabled").value(false));
        ResultActions custom = call(post("/api/trivias").content("""
                {"title":"Bonus 250","questions":[%s],"scoring":{"fastestBonusPoints":250},"events":{"mode":"OFF"}}""".formatted(q)), teacher)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.scoring.fastestBonusPoints").value(250));

        for (ResultActions created : new ResultActions[]{off, custom}) {
            Long triviaId = longValue(json(created, "$.id"));
            Long qid = longValue(json(created, "$.questions[0].id"));
            Long ok = longValue(json(created, "$.questions[0].answers[0].id"));
            ResultActions sess = call(post("/api/sessions").content("{\"triviaId\":" + triviaId + "}"), teacher);
            SessionInfo s = new SessionInfo(longValue(json(sess, "$.id")), json(sess, "$.joinCode"));
            TestUser ana = student("Ana");
            join(ana, s);
            start(teacher, s);
            int points = (int) json(answer(ana, s, qid, ok), "$.totalScore");
            advance(teacher, s).andExpect(jsonPath("$.status").value("FINISHED"));
            int expected = created == off ? points : points + 250;
            assertThat(score(ana, s)).isEqualTo(expected);
        }
    }

    @Test
    void teacherSeesEachStudentsCorrectAndWrongAnswers() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana Ruiz");
        TestUser beto = student("Beto");
        TriviaInfo t = trivia(teacher, 3);
        SessionInfo s = session(teacher, t, false, 2);
        join(ana, s);
        join(beto, s);
        start(teacher, s);
        answer(ana, s, t.questions().get(0).id(), t.questions().get(0).correctAnswerId());
        advance(teacher, s);
        answer(ana, s, t.questions().get(1).id(), t.questions().get(1).wrongAnswerId());
        advance(teacher, s);
        // Pregunta 3 sin responder.
        advance(teacher, s).andExpect(jsonPath("$.status").value("FINISHED"));
        Long anaId = participants.findBySessionIdAndUserId(s.id(), ana.id()).orElseThrow().getId();

        call(get("/api/sessions/" + s.id() + "/participants/" + anaId + "/report"), teacher).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ana Ruiz"))
                .andExpect(jsonPath("$.correctAnswers").value(1))
                .andExpect(jsonPath("$.wrongAnswers").value(1))
                .andExpect(jsonPath("$.unanswered").value(1))
                .andExpect(jsonPath("$.questions[0].outcome").value("ANSWERED_CORRECT"))
                .andExpect(jsonPath("$.questions[0].chosenAnswer").value("Correcta"))
                .andExpect(jsonPath("$.questions[0].fastest").value(true))
                .andExpect(jsonPath("$.questions[1].outcome").value("ANSWERED_WRONG"))
                .andExpect(jsonPath("$.questions[1].chosenAnswer").value("Incorrecta"))
                .andExpect(jsonPath("$.questions[1].correctAnswer").value("Correcta"))
                .andExpect(jsonPath("$.questions[2].outcome").value("UNANSWERED"))
                .andExpect(jsonPath("$.bonuses[0].reason").value("FASTEST_ANSWER"));

        // La propia alumna puede ver su reporte; otro alumno no.
        call(get("/api/sessions/" + s.id() + "/participants/" + anaId + "/report"), ana).andExpect(status().isOk());
        call(get("/api/sessions/" + s.id() + "/participants/" + anaId + "/report"), beto).andExpect(status().isForbidden());
        call(get("/api/sessions/" + s.id() + "/participants/" + anaId + "/report"), teacher()).andExpect(status().isForbidden());
        call(get("/api/sessions/" + s.id() + "/results"), teacher).andExpect(jsonPath("$.viewerRole").value("HOST"));
        call(get("/api/sessions/" + s.id() + "/results"), beto).andExpect(jsonPath("$.viewerRole").value("PARTICIPANT"));
    }

    @Test
    void reportOfParticipantFromAnotherSessionIsNotFound() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        SessionInfo s1 = session(teacher, trivia(teacher, 1), false, 2);
        SessionInfo s2 = session(teacher, trivia(teacher, 1), false, 2);
        join(ana, s2);
        Long anaInS2 = participants.findBySessionIdAndUserId(s2.id(), ana.id()).orElseThrow().getId();
        call(get("/api/sessions/" + s1.id() + "/participants/" + anaInS2 + "/report"), teacher).andExpect(status().isNotFound());
    }
}
