package com.triviaquick.service;

import com.triviaquick.model.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreRulesTest {

    private static Trivia triviaWithDefaults() {
        Trivia t = new Trivia("T", null, null, Instant.EPOCH);
        t.getSpeedTiers().add(new SpeedBonusTier(t, 25, 100));
        t.getSpeedTiers().add(new SpeedBonusTier(t, 60, 50));
        t.getSpeedTiers().add(new SpeedBonusTier(t, 100, 10));
        t.getStreakTiers().add(new StreakBonusTier(t, 3, 30));
        t.getStreakTiers().add(new StreakBonusTier(t, 5, 60));
        return t;
    }

    @Test
    void elapsedPercentUsesServerMeasuredTime() {
        assertThat(ScoreService.elapsedPercent(20, 5_000)).isEqualTo(25.0);
        assertThat(ScoreService.elapsedPercent(20, -100)).isEqualTo(0.0);
    }

    @Test
    void speedBonusFallsThroughTiersAndIsZeroWhenTimeIsUp() {
        Trivia t = triviaWithDefaults();
        assertThat(t.speedBonusFor(0)).isEqualTo(100);
        assertThat(t.speedBonusFor(25)).isEqualTo(100);
        assertThat(t.speedBonusFor(25.1)).isEqualTo(50);
        assertThat(t.speedBonusFor(60)).isEqualTo(50);
        assertThat(t.speedBonusFor(99)).isEqualTo(10);
        assertThat(t.speedBonusFor(100.5)).isZero();
    }

    @Test
    void speedBonusCanBeDisabled() {
        Trivia t = triviaWithDefaults();
        t.updateDetails("T", null, null, TriviaStatus.DRAFT, PlayMode.GROUP, null, null,
                new ScoringConfig(1000, false, true, true), Instant.EPOCH);
        assertThat(t.speedBonusFor(0)).isZero();
    }

    @Test
    void answerScoreAppliesMultiplierToBaseAndSpeed() {
        assertThat(new ScoreService.AnswerScore(200, 100, 2).total()).isEqualTo(600);
        assertThat(new ScoreService.AnswerScore(200, 0, 1).total()).isEqualTo(200);
    }

    @Test
    void streakBonusOnlyAtConfiguredLengths() {
        Trivia t = triviaWithDefaults();
        assertThat(t.streakBonusFor(2)).isZero();
        assertThat(t.streakBonusFor(3)).isEqualTo(30);
        assertThat(t.streakBonusFor(4)).isZero();
        assertThat(t.streakBonusFor(5)).isEqualTo(60);
    }

    private static SessionParticipant participant() {
        User u = new User("a@b.c", "Ana", "x", Role.STUDENT, Instant.EPOCH);
        return new SessionParticipant(null, u, Instant.EPOCH);
    }

    @Test
    void streakGrowsWithConsecutiveCorrectAnswers() {
        SessionParticipant p = participant();
        assertThat(p.updateStreak(true, 0, true)).isEqualTo(1);
        assertThat(p.updateStreak(true, 1, true)).isEqualTo(2);
        assertThat(p.updateStreak(true, 2, true)).isEqualTo(3);
        assertThat(p.getBestStreak()).isEqualTo(3);
    }

    @Test
    void wrongAnswerOrSkippedQuestionBreaksStreakWhenConfigured() {
        SessionParticipant p = participant();
        p.updateStreak(true, 0, true);
        p.updateStreak(true, 1, true);
        assertThat(p.updateStreak(false, 2, true)).isZero();
        assertThat(p.updateStreak(true, 3, true)).isEqualTo(1);
        // Pregunta 4 sin responder: la siguiente acertada reinicia la racha.
        assertThat(p.updateStreak(true, 5, true)).isEqualTo(1);
        assertThat(p.getBestStreak()).isEqualTo(2);
    }

    @Test
    void streakIsKeptWhenMissesDoNotBreakIt() {
        SessionParticipant p = participant();
        p.updateStreak(true, 0, false);
        assertThat(p.updateStreak(false, 1, false)).isEqualTo(1);
        assertThat(p.updateStreak(true, 3, false)).isEqualTo(2);
    }

    private static Optional<GameEventResponse> response(boolean correct, int millis) {
        GameEventResponse r = Mockito.mock(GameEventResponse.class);
        Mockito.when(r.isCorrect()).thenReturn(correct);
        Mockito.when(r.getResponseTimeMs()).thenReturn(millis);
        return Optional.of(r);
    }

    @Test
    void duelWinnerRules() {
        assertThat(GameEventService.duelWinner(1L, response(true, 900), 2L, response(false, 100))).isEqualTo(1L);
        assertThat(GameEventService.duelWinner(1L, response(true, 900), 2L, response(true, 400))).isEqualTo(2L);
        assertThat(GameEventService.duelWinner(1L, response(false, 900), 2L, Optional.empty())).isNull();
        assertThat(GameEventService.duelWinner(1L, response(true, 500), 2L, response(true, 500))).isNull();
        assertThat(GameEventService.duelWinner(1L, Optional.empty(), 2L, response(true, 5000))).isEqualTo(2L);
    }

    @Test
    void manualEventOverridesQuizDefaults() {
        EventConfig c = new EventConfig();
        TimelineEvent t = new TimelineEvent(null, 1, GameEventType.DUEL, 30, null, null, 400, null, null);
        GameEventService.EventParams p = GameEventService.paramsFor(GameEventType.DUEL, c, t);
        assertThat(p.seconds()).isEqualTo(30);
        assertThat(p.stake()).isEqualTo(400);
        GameEventService.EventParams defaults = GameEventService.paramsFor(GameEventType.DUEL, c, null);
        assertThat(defaults.seconds()).isEqualTo(20);
        assertThat(defaults.stake()).isEqualTo(250);
    }
}
