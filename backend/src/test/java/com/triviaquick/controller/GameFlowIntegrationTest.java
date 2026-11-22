package com.triviaquick.controller;

import com.triviaquick.IntegrationTestBase;
import com.triviaquick.repository.ParticipantRepository;
import com.triviaquick.repository.ScoreTransactionRepository;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameFlowIntegrationTest extends IntegrationTestBase {

    @Autowired ParticipantRepository participants;
    @Autowired ScoreTransactionRepository transactions;

    // 4. Una respuesta correcta genera la puntuación correspondiente.
    @Test
    void correctAnswerScoresByServerRules() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        TriviaInfo t = trivia(teacher, 2);
        SessionInfo s = session(teacher, t, false, 2);
        join(ana, s);
        start(teacher, s).andExpect(status().isOk());

        clock.advance(Duration.ofSeconds(10)); // 50 % del tiempo → 1000 base + 50 de velocidad
        answer(ana, s, t.questions().get(0).id(), t.questions().get(0).correctAnswerId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.pointsAwarded").value(1050))
                .andExpect(jsonPath("$.speedBonus").value(50))
                .andExpect(jsonPath("$.totalScore").value(1050));
        assertThat(score(ana, s)).isEqualTo(1050);
    }

    @Test
    void wrongAnswerScoresZero() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        TriviaInfo t = trivia(teacher, 1);
        SessionInfo s = session(teacher, t, false, 2);
        join(ana, s);
        start(teacher, s);
        answer(ana, s, t.questions().get(0).id(), t.questions().get(0).wrongAnswerId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.pointsAwarded").value(0));
    }

    // 5. Una respuesta duplicada no suma puntos dos veces.
    @Test
    void duplicateAnswerDoesNotScoreTwice() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        TriviaInfo t = trivia(teacher, 2);
        SessionInfo s = session(teacher, t, false, 2);
        join(ana, s);
        start(teacher, s);
        QuestionInfo q = t.questions().get(0);
        answer(ana, s, q.id(), q.correctAnswerId()).andExpect(status().isOk());
        answer(ana, s, q.id(), q.correctAnswerId()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANSWER_ALREADY_SUBMITTED"));
        answer(ana, s, q.id(), q.wrongAnswerId()).andExpect(status().isConflict());
        assertThat(score(ana, s)).isEqualTo(1100);
    }

    @Test
    void answersMustTargetTheActiveQuestionAndItsOwnOptions() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        TriviaInfo t = trivia(teacher, 2);
        TriviaInfo other = trivia(teacher, 1);
        SessionInfo s = session(teacher, t, false, 2);
        join(ana, s);
        start(teacher, s);
        // Pregunta que aún no está en curso.
        answer(ana, s, t.questions().get(1).id(), t.questions().get(1).correctAnswerId())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("QUESTION_NOT_ACTIVE"));
        // Opción de otra trivia.
        answer(ana, s, t.questions().get(0).id(), other.questions().get(0).correctAnswerId())
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ANSWER_NOT_IN_QUESTION"));
        // Fuera de plazo.
        clock.advance(Duration.ofSeconds(25));
        answer(ana, s, t.questions().get(0).id(), t.questions().get(0).correctAnswerId())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TIME_EXPIRED"));
        assertThat(score(ana, s)).isZero();
    }

    // 9 y 10. Empates y correspondencia entre clasificación y puntuación real.
    @Test
    void rankingReflectsRealScoresAndSharesTiedPositions() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        TestUser beto = student("Beto");
        TestUser cris = student("Cris");
        TestUser dani = student("Dani");
        TriviaInfo t = trivia(teacher, 2);
        SessionInfo s = session(teacher, t, false, 2);
        for (TestUser u : List.of(ana, beto, cris, dani)) {
            join(u, s);
        }
        start(teacher, s);
        QuestionInfo q1 = t.questions().get(0);
        answer(ana, s, q1.id(), q1.correctAnswerId());
        answer(beto, s, q1.id(), q1.correctAnswerId());
        answer(cris, s, q1.id(), q1.correctAnswerId());
        answer(dani, s, q1.id(), q1.wrongAnswerId());
        advance(teacher, s).andExpect(status().isOk());
        QuestionInfo q2 = t.questions().get(1);
        answer(ana, s, q2.id(), q2.correctAnswerId());
        clock.advance(Duration.ofSeconds(8)); // 40 % → +50 en lugar de +100
        answer(beto, s, q2.id(), q2.correctAnswerId());
        answer(cris, s, q2.id(), q2.correctAnswerId());

        // Provisional: Ana 2200; Beto y Cris 2150 empatados (misma puntuación y aciertos); Dani 0.
        ResultActions provisional = call(get("/api/sessions/" + s.id() + "/ranking"), beto).andExpect(status().isOk())
                .andExpect(jsonPath("$.provisional").value(true));
        assertThat((JSONArray) json(provisional, "$.entries[*].position")).containsExactly(1, 2, 2, 4);

        advance(teacher, s).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINISHED"));

        ResultActions results = call(get("/api/sessions/" + s.id() + "/results"), beto).andExpect(status().isOk())
                .andExpect(jsonPath("$.ranking[0].displayName").value("Ana"))
                .andExpect(jsonPath("$.ranking[0].score").value(2200))
                .andExpect(jsonPath("$.ranking[1].position").value(2))
                .andExpect(jsonPath("$.ranking[2].position").value(2))
                .andExpect(jsonPath("$.ranking[1].tied").value(true))
                .andExpect(jsonPath("$.ranking[3].position").value(4))
                .andExpect(jsonPath("$.myResult.displayName").value("Beto"))
                .andExpect(jsonPath("$.podium.places.length()").value(2))
                .andExpect(jsonPath("$.podium.places[1].participants.length()").value(2));
        // La puntuación de cada fila coincide con el estado real del participante.
        for (TestUser u : List.of(ana, beto, cris, dani)) {
            int real = participants.findBySessionIdAndUserId(s.id(), u.id()).orElseThrow().getScore();
            List<Map<String, Object>> rows = json(results, "$.ranking[?(@.displayName == '" + u.name() + "')]");
            assertThat(rows.get(0).get("score")).isEqualTo(real);
        }
        // La clasificación corresponde exclusivamente a la sesión consultada.
        assertThat((JSONArray) json(results, "$.ranking[*].displayName")).hasSize(4);
        call(get("/api/sessions/" + s.id() + "/podium"), teacher).andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].participants[0].displayName").value("Ana"));
    }

    // 11. Una partida finalizada no admite nuevas respuestas.
    @Test
    void finishedSessionRejectsAnswersAndKeepsResults() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        TriviaInfo t = trivia(teacher, 3);
        SessionInfo s = session(teacher, t, false, 2);
        join(ana, s);
        start(teacher, s);
        QuestionInfo q = t.questions().get(0);
        answer(ana, s, q.id(), q.correctAnswerId()).andExpect(status().isOk());
        call(post("/api/sessions/" + s.id() + "/finish"), teacher).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        answer(ana, s, t.questions().get(1).id(), t.questions().get(1).correctAnswerId())
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSION_FINISHED"));
        advance(teacher, s).andExpect(status().isConflict());
        call(post("/api/sessions/" + s.id() + "/finish"), teacher).andExpect(status().isConflict());
        call(get("/api/sessions/" + s.id() + "/results"), ana).andExpect(status().isOk())
                .andExpect(jsonPath("$.ranking[0].score").value(1100));
    }

    @Test
    void resultsAreNotAvailableBeforeTheEnd() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        SessionInfo s = session(teacher, trivia(teacher, 1), false, 2);
        join(ana, s);
        call(get("/api/sessions/" + s.id() + "/results"), ana).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESULTS_NOT_READY"));
    }

    @Test
    void sessionWithoutAnswersFinishesWithEveryoneTiedAtZero() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        TestUser beto = student("Beto");
        TriviaInfo t = trivia(teacher, 1);
        SessionInfo s = session(teacher, t, false, 2);
        join(ana, s);
        join(beto, s);
        start(teacher, s);
        advance(teacher, s).andExpect(jsonPath("$.status").value("FINISHED"));
        call(get("/api/sessions/" + s.id() + "/results"), teacher).andExpect(status().isOk())
                .andExpect(jsonPath("$.myResult").doesNotExist())
                .andExpect(jsonPath("$.ranking[0].position").value(1))
                .andExpect(jsonPath("$.ranking[1].position").value(1))
                .andExpect(jsonPath("$.podium.places.length()").value(1));
    }

    /** Partida completa con los cuatro minijuegos; la puntuación final cuadra con el historial. */
    @Test
    void fullGameWithAllMinigamesKeepsScoresConsistent() throws Exception {
        TestUser teacher = teacher();
        TestUser ana = student("Ana");
        TestUser beto = student("Beto");
        TriviaInfo t = trivia(teacher, 5);
        SessionInfo s = session(teacher, t, true, 1);
        join(ana, s);
        join(beto, s);
        start(teacher, s);
        String state = "/api/sessions/" + s.id() + "/state";

        // P1: Ana acierta, Beto falla.
        answer(ana, s, t.questions().get(0).id(), t.questions().get(0).correctAnswerId());
        answer(beto, s, t.questions().get(0).id(), t.questions().get(0).wrongAnswerId());
        // Evento 1: reto relámpago.
        advance(teacher, s).andExpect(jsonPath("$.status").value("EVENT"))
                .andExpect(jsonPath("$.currentEvent.type").value("LIGHTNING_CHALLENGE"));
        ResultActions ev = call(get("/api/sessions/" + s.id() + "/events/current"), beto)
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.challenge.prompt").exists());
        Long lightningId = longValue(json(ev, "$.id"));
        int options = ((JSONArray) json(ev, "$.challenge.options")).size();
        int betoBefore = score(beto, s);
        // Beto prueba opciones de una en una: solo se acepta la primera respuesta.
        call(post("/api/sessions/" + s.id() + "/events/" + lightningId + "/responses").content("{\"optionIndex\":0}"), beto)
                .andExpect(status().isOk());
        call(post("/api/sessions/" + s.id() + "/events/" + lightningId + "/responses").content("{\"optionIndex\":1}"), beto)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EVENT_ALREADY_ANSWERED"));
        int betoAfter = score(beto, s);
        assertThat(betoAfter - betoBefore).isIn(0, 300);
        assertThat(options).isGreaterThan(1);

        // P2.
        advance(teacher, s).andExpect(jsonPath("$.status").value("QUESTION"));
        answer(ana, s, t.questions().get(1).id(), t.questions().get(1).correctAnswerId());
        answer(beto, s, t.questions().get(1).id(), t.questions().get(1).correctAnswerId());
        // Evento 2: multiplicador para la mitad inferior.
        advance(teacher, s).andExpect(jsonPath("$.currentEvent.type").value("MULTIPLIER"));
        String lowest = score(ana, s) <= score(beto, s) ? "ana" : "beto";
        TestUser low = lowest.equals("ana") ? ana : beto;
        TestUser high = low == ana ? beto : ana;
        ResultActions lowState = call(get(state), low).andExpect(jsonPath("$.myPowerUps[0].type").value("MULTIPLIER"));
        call(get(state), high).andExpect(jsonPath("$.myPowerUps.length()").value(0));
        Long multiplierId = longValue(json(lowState, "$.myPowerUps[0].id"));
        // El otro jugador no puede activar una habilidad ajena.
        call(post("/api/sessions/" + s.id() + "/power-ups/" + multiplierId + "/activate"), high).andExpect(status().isNotFound());
        call(post("/api/sessions/" + s.id() + "/power-ups/" + multiplierId + "/activate"), low)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        // P3: el multiplicador duplica y se consume.
        advance(teacher, s);
        answer(low, s, t.questions().get(2).id(), t.questions().get(2).correctAnswerId())
                .andExpect(jsonPath("$.multiplierApplied").value(true))
                .andExpect(jsonPath("$.pointsAwarded").value(2200));
        answer(high, s, t.questions().get(2).id(), t.questions().get(2).wrongAnswerId());
        call(get(state), low).andExpect(jsonPath("$.myPowerUps[0].status").value("CONSUMED"));

        // Evento 3: escudo para quien acertó P3.
        advance(teacher, s).andExpect(jsonPath("$.currentEvent.type").value("SHIELD"));
        ResultActions shieldState = call(get(state), low).andExpect(jsonPath("$.myPowerUps[1].type").value("SHIELD"));
        Long shieldId = longValue(json(shieldState, "$.myPowerUps[1].id"));
        call(post("/api/sessions/" + s.id() + "/power-ups/" + shieldId + "/activate"), low).andExpect(status().isOk());

        // P4 y evento 4: duelo entre los dos.
        advance(teacher, s);
        answer(ana, s, t.questions().get(3).id(), t.questions().get(3).correctAnswerId());
        answer(beto, s, t.questions().get(3).id(), t.questions().get(3).correctAnswerId());
        ResultActions duel = advance(teacher, s).andExpect(jsonPath("$.currentEvent.type").value("DUEL"))
                .andExpect(jsonPath("$.currentEvent.duelists.length()").value(2));
        Long duelId = longValue(json(duel, "$.currentEvent.id"));
        // El anfitrión no puede responder el duelo.
        call(post("/api/sessions/" + s.id() + "/events/" + duelId + "/responses").content("{\"optionIndex\":0}"), teacher)
                .andExpect(status().isForbidden());
        call(post("/api/sessions/" + s.id() + "/events/" + duelId + "/responses").content("{\"optionIndex\":0}"), ana)
                .andExpect(status().isOk());
        call(post("/api/sessions/" + s.id() + "/events/" + duelId + "/responses").content("{\"optionIndex\":1}"), beto)
                .andExpect(status().isOk());

        // P5 y cierre.
        advance(teacher, s).andExpect(jsonPath("$.status").value("QUESTION"));
        advance(teacher, s).andExpect(jsonPath("$.status").value("FINISHED"));

        // Invariante: puntuación == suma del historial y nunca negativa.
        for (TestUser u : List.of(ana, beto)) {
            var p = participants.findBySessionIdAndUserId(s.id(), u.id()).orElseThrow();
            assertThat(p.getScore()).isGreaterThanOrEqualTo(0);
            assertThat((long) p.getScore()).isEqualTo(transactions.sumDeltaByParticipant(p.getId()));
        }
        call(get("/api/sessions/" + s.id() + "/score-transactions"), teacher).andExpect(status().isOk());
        call(get("/api/sessions/" + s.id() + "/results"), ana).andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(2));
    }
}
