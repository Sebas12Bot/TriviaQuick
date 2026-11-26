package com.triviaquick.service;

import com.triviaquick.IntegrationTestBase;
import com.triviaquick.dto.SessionDtos.SubmitAnswerDTO;
import com.triviaquick.exception.ApiException;
import com.triviaquick.model.*;
import com.triviaquick.repository.*;
import com.triviaquick.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Reglas de integridad de puntos sobre PostgreSQL real: escudo, límites, multiplicador y concurrencia. */
class ScoreIntegrityIntegrationTest extends IntegrationTestBase {

    @Autowired TransactionTemplate tx;
    @Autowired ScoreService scoreService;
    @Autowired GameSessionService sessionService;
    @Autowired ParticipantRepository participants;
    @Autowired GameSessionRepository sessions;
    @Autowired GameEventRepository events;
    @Autowired PowerUpRepository powerUps;
    @Autowired ScoreTransactionRepository transactions;

    private record Duo(SessionInfo session, Long p1, Long p2, TestUser u1, TestUser u2, TriviaInfo trivia, TestUser teacher) {
    }

    private Duo duo(int questions) throws Exception {
        TestUser teacher = teacher();
        TestUser a = student("Ana");
        TestUser b = student("Beto");
        TriviaInfo t = trivia(teacher, questions);
        SessionInfo s = session(teacher, t, false, 2);
        join(a, s);
        join(b, s);
        Long p1 = participants.findBySessionIdAndUserId(s.id(), a.id()).orElseThrow().getId();
        Long p2 = participants.findBySessionIdAndUserId(s.id(), b.id()).orElseThrow().getId();
        return new Duo(s, p1, p2, a, b, t, teacher);
    }

    private Long duelEvent(Duo d, int slot) {
        return tx.execute(st -> {
            GameSession session = sessions.findById(d.session().id()).orElseThrow();
            GameEvent e = new GameEvent(session, GameEventType.DUEL, slot, clock.instant(), clock.instant().plusSeconds(20));
            e.setDuelists(participants.getReferenceById(d.p1()), participants.getReferenceById(d.p2()));
            return events.saveAndFlush(e).getId();
        });
    }

    private void give(Long participantId, int points, String key) {
        tx.executeWithoutResult(st -> scoreService.award(participants.findByIdForUpdate(participantId).orElseThrow(),
                points, ScoreReason.LIGHTNING_BONUS, null, null, "Prueba", key));
    }

    private ScoreService.TransferOutcome transfer(Long fromId, Long toId, int amount, Long eventId, String key) {
        return tx.execute(st -> {
            List<SessionParticipant> locked = participants.findAllByIdForUpdate(new ArrayList<>(List.of(fromId, toId)));
            SessionParticipant from = locked.stream().filter(p -> p.getId().equals(fromId)).findFirst().orElseThrow();
            SessionParticipant to = locked.stream().filter(p -> p.getId().equals(toId)).findFirst().orElseThrow();
            return scoreService.transfer(from, to, amount, events.getReferenceById(eventId), key);
        });
    }

    private int scoreOf(Long participantId) {
        return participants.findById(participantId).orElseThrow().getScore();
    }

    private void assertLedgerMatches(Long... participantIds) {
        for (Long id : participantIds) {
            assertThat((long) scoreOf(id)).isEqualTo(transactions.sumDeltaByParticipant(id));
            assertThat(scoreOf(id)).isGreaterThanOrEqualTo(0);
        }
    }

    // 7. El escudo bloquea una transferencia válida y se consume.
    @Test
    void activeShieldBlocksOneTransferAndIsConsumed() throws Exception {
        Duo d = duo(1);
        give(d.p1(), 500, "seed-shield-" + d.p1());
        Long eventId = duelEvent(d, 0);
        Long shieldId = tx.execute(st -> {
            PlayerPowerUp shield = new PlayerPowerUp(participants.getReferenceById(d.p1()), PowerUpType.SHIELD,
                    events.getReferenceById(eventId), clock.instant());
            shield.activate(clock.instant());
            return powerUps.save(shield).getId();
        });

        ScoreService.TransferOutcome first = transfer(d.p1(), d.p2(), 250, eventId, "T1-" + eventId);
        assertThat(first.blockedByShield()).isTrue();
        assertThat(scoreOf(d.p1())).isEqualTo(500);
        assertThat(scoreOf(d.p2())).isZero();
        assertThat(powerUps.findById(shieldId).orElseThrow().getStatus()).isEqualTo(PowerUpStatus.CONSUMED);

        ScoreService.TransferOutcome second = transfer(d.p1(), d.p2(), 250, eventId, "T2-" + eventId);
        assertThat(second.blockedByShield()).isFalse();
        assertThat(second.amount()).isEqualTo(250);
        assertThat(scoreOf(d.p1())).isEqualTo(250);
        assertThat(scoreOf(d.p2())).isEqualTo(250);
        assertLedgerMatches(d.p1(), d.p2());
    }

    @Test
    void availableButInactiveShieldDoesNotBlock() throws Exception {
        Duo d = duo(1);
        give(d.p1(), 300, "seed-inactive-" + d.p1());
        Long eventId = duelEvent(d, 0);
        tx.executeWithoutResult(st -> powerUps.save(new PlayerPowerUp(participants.getReferenceById(d.p1()),
                PowerUpType.SHIELD, events.getReferenceById(eventId), clock.instant())));
        assertThat(transfer(d.p1(), d.p2(), 250, eventId, "T-inactive-" + eventId).blockedByShield()).isFalse();
        assertThat(scoreOf(d.p1())).isEqualTo(50);
    }

    // 8. Un jugador nunca pierde más puntos de los que tiene.
    @Test
    void transferIsCappedByAvailablePoints() throws Exception {
        Duo d = duo(1);
        give(d.p1(), 100, "seed-cap-" + d.p1());
        Long eventId = duelEvent(d, 0);
        ScoreService.TransferOutcome t = transfer(d.p1(), d.p2(), 250, eventId, "T-cap-" + eventId);
        assertThat(t.amount()).isEqualTo(100);
        assertThat(scoreOf(d.p1())).isZero();
        assertThat(scoreOf(d.p2())).isEqualTo(100);
        // Sin saldo, una nueva transferencia no mueve nada ni deja saldos negativos.
        assertThat(transfer(d.p1(), d.p2(), 250, eventId, "T-cap2-" + eventId).amount()).isZero();
        assertLedgerMatches(d.p1(), d.p2());
    }

    @Test
    void sameGameFactCannotBeAppliedTwice() throws Exception {
        Duo d = duo(1);
        give(d.p1(), 100, "once-" + d.p1());
        assertThatThrownBy(() -> give(d.p1(), 100, "once-" + d.p1())).isInstanceOf(IllegalStateException.class);
        assertThat(scoreOf(d.p1())).isEqualTo(100);
    }

    // 6. El multiplicador se aplica una sola vez.
    @Test
    void multiplierAppliesToNextAnswerOnlyOnce() throws Exception {
        Duo d = duo(3);
        Long eventId = duelEvent(d, 99);
        Long multiplierId = tx.execute(st -> powerUps.save(new PlayerPowerUp(participants.getReferenceById(d.p1()),
                PowerUpType.MULTIPLIER, events.getReferenceById(eventId), clock.instant())).getId());
        start(d.teacher(), d.session()).andExpect(status().isOk());
        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/sessions/" + d.session().id() + "/power-ups/" + multiplierId + "/activate"), d.u1())
                .andExpect(status().isOk());
        // Activarlo de nuevo no es posible.
        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/sessions/" + d.session().id() + "/power-ups/" + multiplierId + "/activate"), d.u1())
                .andExpect(status().isConflict());

        QuestionInfo q1 = d.trivia().questions().get(0);
        answer(d.u1(), d.session(), q1.id(), q1.correctAnswerId()).andExpect(status().isOk());
        advance(d.teacher(), d.session());
        QuestionInfo q2 = d.trivia().questions().get(1);
        answer(d.u1(), d.session(), q2.id(), q2.correctAnswerId()).andExpect(status().isOk());

        // P1 x2, P2 normal y el bonus de la más rápida de P1 (al avanzar); P2 sigue abierta.
        assertThat(scoreOf(d.p1())).isEqualTo((1000 + 100) * 2 + (1000 + 100) + 100);
        assertThat(powerUps.findById(multiplierId).orElseThrow().getStatus()).isEqualTo(PowerUpStatus.CONSUMED);
        assertLedgerMatches(d.p1());
    }

    // 12a. Respuestas idénticas enviadas en paralelo: solo una puntúa.
    @Test
    void concurrentDuplicateAnswersScoreExactlyOnce() throws Exception {
        Duo d = duo(1);
        start(d.teacher(), d.session()).andExpect(status().isOk());
        QuestionInfo q = d.trivia().questions().get(0);
        AuthUser ana = new AuthUser(d.u1().id(), "a", "Ana", Role.STUDENT);
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                try {
                    sessionService.submitAnswer(d.session().id(), ana, new SubmitAnswerDTO(q.id(), q.correctAnswerId()));
                    ok.incrementAndGet();
                } catch (ApiException | org.springframework.dao.DataAccessException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertThat(ok.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(threads - 1);
        assertThat(scoreOf(d.p1())).isEqualTo(1100);
        assertLedgerMatches(d.p1());
    }

    // 12b. Bonificaciones y transferencias cruzadas en paralelo: sin interbloqueos, sin negativos y con el historial cuadrado.
    @Test
    void concurrentCrossTransfersKeepLedgerConsistent() throws Exception {
        Duo d = duo(1);
        give(d.p1(), 1000, "seed-cc1-" + d.p1());
        give(d.p2(), 1000, "seed-cc2-" + d.p2());
        Long eventId = duelEvent(d, 0);
        int ops = 40;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < ops; i++) {
            final int n = i;
            futures.add(pool.submit(() -> {
                go.await();
                if (n % 3 == 0) {
                    give(n % 2 == 0 ? d.p1() : d.p2(), 10, "cc-bonus-" + eventId + "-" + n);
                } else if (n % 2 == 0) {
                    transfer(d.p1(), d.p2(), 120, eventId, "cc-" + eventId + "-" + n);
                } else {
                    transfer(d.p2(), d.p1(), 120, eventId, "cc-" + eventId + "-" + n);
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
        int bonuses = (int) java.util.stream.IntStream.range(0, ops).filter(n -> n % 3 == 0).count();
        assertThat(scoreOf(d.p1()) + scoreOf(d.p2())).isEqualTo(2000 + bonuses * 10);
        assertLedgerMatches(d.p1(), d.p2());
    }

    // 11 (concurrencia). El cierre de la partida y las respuestas en vuelo no producen resultados incoherentes.
    @Test
    void finishingWhileAnsweringNeverChangesPersistedResults() throws Exception {
        Duo d = duo(2);
        start(d.teacher(), d.session()).andExpect(status().isOk());
        QuestionInfo q = d.trivia().questions().get(0);
        AuthUser ana = new AuthUser(d.u1().id(), "a", "Ana", Role.STUDENT);
        AuthUser beto = new AuthUser(d.u2().id(), "b", "Beto", Role.STUDENT);
        AuthUser host = new AuthUser(d.teacher().id(), "t", "Docente", Role.TEACHER);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Object> answerA = () -> { go.await(); try { return sessionService.submitAnswer(d.session().id(), ana, new SubmitAnswerDTO(q.id(), q.correctAnswerId())); } catch (ApiException e) { return e; } };
        Callable<Object> answerB = () -> { go.await(); try { return sessionService.submitAnswer(d.session().id(), beto, new SubmitAnswerDTO(q.id(), q.correctAnswerId())); } catch (ApiException e) { return e; } };
        Callable<Object> finish = () -> { go.await(); return sessionService.finish(d.session().id(), host); };
        List<Future<Object>> fs = List.of(pool.submit(answerA), pool.submit(answerB), pool.submit(finish));
        go.countDown();
        for (Future<Object> f : fs) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();
        // Los resultados persistidos coinciden con la puntuación final: nada entró después del cierre.
        var results = call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/sessions/" + d.session().id() + "/results"), d.teacher()).andExpect(status().isOk());
        net.minidev.json.JSONArray scores = json(results, "$.ranking[?(@.participantId == " + d.p1() + ")].score");
        assertThat(scores.get(0)).isEqualTo(scoreOf(d.p1()));
        assertLedgerMatches(d.p1(), d.p2());
    }
}
