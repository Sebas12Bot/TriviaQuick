package com.triviaquick.mapper;

import com.triviaquick.dto.TriviaDtos.*;
import com.triviaquick.model.EventConfig;
import com.triviaquick.model.ScoringConfig;
import com.triviaquick.model.TimelineEvent;
import com.triviaquick.model.Trivia;

import java.util.EnumSet;

public final class TriviaMapper {

    private TriviaMapper() {
    }

    public static String mediaUrl(String key) {
        return com.triviaquick.service.media.MediaUrls.of(key);
    }

    public static TriviaSummaryDTO toSummary(Trivia t, long sessionsPlayed) {
        return new TriviaSummaryDTO(t.getId(), t.getTitle(), t.getDescription(), t.getCategory(), t.getStatus(),
                t.getPlayMode(), t.getQuestions().size(), mediaUrl(t.getCoverImageKey()), t.getCreatedAt(),
                t.getUpdatedAt(), sessionsPlayed, sessionsPlayed > 0);
    }

    public static TriviaDetailDTO toDetail(Trivia t, boolean questionsLocked, boolean inUse) {
        ScoringConfig s = t.getScoring();
        EventConfig e = t.getEvents();
        ScoringDTO scoring = new ScoringDTO(s.getDefaultPoints(), s.isSpeedBonusEnabled(),
                t.getSpeedTiers().stream().map(x -> new SpeedTierDTO(x.getMaxElapsedPercent(), x.getBonusPoints())).toList(),
                s.isStreakEnabled(), s.isStreakBreaksOnMiss(),
                t.getStreakTiers().stream().map(x -> new StreakTierDTO(x.getStreakLength(), x.getBonusPoints())).toList(),
                s.isFastestBonusEnabled(), s.getFastestBonusPoints());
        EventSettingsDTO events = new EventSettingsDTO(e.getMode(), e.getAutoFrequency(),
                e.autoTypesInOrder().isEmpty() ? EnumSet.noneOf(com.triviaquick.model.GameEventType.class)
                        : EnumSet.copyOf(e.autoTypesInOrder()),
                new AbilityConfigDTO(e.getLightningSeconds(), e.getLightningReward(), e.getLightningCategory(),
                        e.getMultiplierFactor(), e.getMultiplierTarget(), e.getShieldTarget(), e.getDuelSeconds(),
                        e.getDuelStake(), e.getPowerUpSeconds()));
        return new TriviaDetailDTO(t.getId(), t.getTitle(), t.getDescription(), t.getCategory(), t.getStatus(),
                t.getPlayMode(), t.getCoverImageKey(), mediaUrl(t.getCoverImageKey()), t.getActivityMinutes(),
                t.getCreatedAt(), t.getUpdatedAt(), questionsLocked, inUse,
                t.getQuestions().stream().map(q -> new QuestionDetailDTO(q.getId(), q.getPosition(), q.getType(),
                        q.getText(), q.getTimeLimitSeconds(), q.getBasePoints(), q.getImageKey(), mediaUrl(q.getImageKey()), q.getVideoKey(), mediaUrl(q.getVideoKey()),
                        q.getAnswers().stream().map(a -> new AnswerDetailDTO(a.getId(), a.getText(), a.isCorrect())).toList()))
                        .toList(),
                scoring, events,
                t.getTimeline().stream().map(TriviaMapper::toDto).toList());
    }

    public static TimelineEventDTO toDto(TimelineEvent e) {
        return new TimelineEventDTO(e.getAfterQuestion(), e.getType(), e.getDurationSeconds(), e.getRewardPoints(),
                e.getMultiplierFactor(), e.getStake(), e.getChallengeCategory(), e.getTarget());
    }
}
