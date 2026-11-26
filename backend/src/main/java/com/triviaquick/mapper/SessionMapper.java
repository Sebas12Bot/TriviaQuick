package com.triviaquick.mapper;

import com.triviaquick.dto.SessionDtos.*;
import com.triviaquick.model.GameSession;
import com.triviaquick.model.Question;
import com.triviaquick.model.SessionParticipant;
import com.triviaquick.model.SubmittedAnswer;

public final class SessionMapper {

    private SessionMapper() {
    }

    public static SessionSummaryDTO toSummary(GameSession s, boolean host, long participantCount) {
        return new SessionSummaryDTO(s.getId(), s.getTrivia().getTitle(), s.getStatus(),
                host ? ViewerRole.HOST : ViewerRole.PARTICIPANT, host ? s.getJoinCode() : null,
                participantCount, s.getCreatedAt(), s.getFinishedAt());
    }

    /** Pregunta en curso sin revelar qué opción es correcta. */
    public static QuestionViewDTO toQuestionView(Question q, int total, GameSession s) {
        return new QuestionViewDTO(q.getId(), q.getPosition(), total, q.getText(),
                q.getAnswers().stream().map(a -> new OptionDTO(a.getId(), a.getText())).toList(),
                q.getTimeLimitSeconds(), q.getBasePoints(), s.getQuestionStartedAt(), s.getQuestionDeadline(),
                q.getType(), TriviaMapper.mediaUrl(q.getImageKey()), TriviaMapper.mediaUrl(q.getVideoKey()));
    }

    public static MyAnswerDTO toMyAnswer(SubmittedAnswer a) {
        return new MyAnswerDTO(a.getAnswer().getId(), a.isCorrect(), a.getPointsAwarded(), a.isMultiplierApplied(),
                a.getSpeedBonus(), a.getMultiplierFactor());
    }

    public static ParticipantBriefDTO toBrief(SessionParticipant p) {
        return new ParticipantBriefDTO(p.getId(), p.getDisplayName(), RankingMapper.initials(p.getDisplayName()));
    }
}
