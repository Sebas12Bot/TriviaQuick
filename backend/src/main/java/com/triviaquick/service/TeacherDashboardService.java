package com.triviaquick.service;

import com.triviaquick.dto.TeacherDashboardDTO;
import com.triviaquick.model.GameResult;
import com.triviaquick.model.GameSession;
import com.triviaquick.model.SessionStatus;
import com.triviaquick.model.Trivia;
import com.triviaquick.model.TriviaStatus;
import com.triviaquick.repository.GameResultRepository;
import com.triviaquick.repository.GameSessionRepository;
import com.triviaquick.repository.ParticipantRepository;
import com.triviaquick.repository.TriviaRepository;
import com.triviaquick.security.AuthUser;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Resumen del panel docente calculado con datos reales: sin cifras de relleno. */
@Service
public class TeacherDashboardService {

    private final TriviaRepository trivias;
    private final GameSessionRepository sessions;
    private final ParticipantRepository participants;
    private final GameResultRepository results;

    public TeacherDashboardService(TriviaRepository trivias, GameSessionRepository sessions,
                                   ParticipantRepository participants, GameResultRepository results) {
        this.trivias = trivias;
        this.sessions = sessions;
        this.participants = participants;
        this.results = results;
    }

    @Transactional(readOnly = true)
    public TeacherDashboardDTO dashboard(AuthUser teacher) {
        List<Trivia> mine = trivias.findByOwnerIdOrderByCreatedAtDesc(teacher.id());
        long published = mine.stream().filter(t -> t.getStatus() == TriviaStatus.PUBLISHED).count();
        long finished = sessions.countByHostIdAndStatus(teacher.id(), SessionStatus.FINISHED);
        List<TeacherDashboardDTO.RecentResultDTO> recent = sessions
                .findRecentFinishedByHost(teacher.id(), PageRequest.of(0, 5)).stream()
                .map(this::toRecent).toList();
        return new TeacherDashboardDTO(mine.size(), published, mine.size() - published, finished, recent);
    }

    private TeacherDashboardDTO.RecentResultDTO toRecent(GameSession s) {
        List<GameResult> rows = results.findOrderedBySession(s.getId());
        List<String> winners = rows.stream().filter(r -> r.getPosition() == 1)
                .map(r -> r.getParticipant().getDisplayName()).toList();
        Integer top = rows.isEmpty() ? null : rows.get(0).getFinalScore();
        return new TeacherDashboardDTO.RecentResultDTO(s.getId(), s.getTrivia().getTitle(), s.getFinishedAt(),
                participants.countBySessionId(s.getId()), winners, top);
    }
}
