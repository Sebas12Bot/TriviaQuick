package com.triviaquick.dto;

import java.time.Instant;
import java.util.List;

public record TeacherDashboardDTO(long totalQuizzes, long publishedQuizzes, long draftQuizzes, long finishedSessions,
                                  List<RecentResultDTO> recentResults) {

    public record RecentResultDTO(Long sessionId, String triviaTitle, Instant finishedAt, long participantCount,
                                  List<String> winners, Integer topScore) {
    }
}
