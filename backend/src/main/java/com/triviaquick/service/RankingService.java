package com.triviaquick.service;

import com.triviaquick.dto.ParticipantRankingDTO;
import com.triviaquick.dto.PodiumDTO;
import com.triviaquick.dto.RankingDTO;
import com.triviaquick.mapper.RankingMapper;
import com.triviaquick.model.GameResult;
import com.triviaquick.model.GameSession;
import com.triviaquick.model.SessionParticipant;
import com.triviaquick.repository.GameResultRepository;
import com.triviaquick.repository.ParticipantRepository;
import com.triviaquick.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Clasificación de una sesión.
 *
 * <p><b>Regla de orden y desempate</b> (documentada también en la API):
 * <ol>
 *   <li>Mayor puntuación total.</li>
 *   <li>Si empatan, mayor número de respuestas correctas.</li>
 *   <li>Si persiste el empate, los participantes comparten la misma posición y la siguiente posición
 *   salta tantos puestos como empatados haya (clasificación de competición: 1, 2, 2, 4).</li>
 * </ol>
 * El nombre solo se usa para que el orden de presentación de los empatados sea estable; nunca decide la posición.
 */
@Service
public class RankingService {

    public static final String TIE_BREAK_RULE =
            "Orden por puntuación total descendente; a igualdad de puntos gana quien tenga más respuestas correctas; "
                    + "si persiste el empate se comparte la posición (1, 2, 2, 4).";

    private final ParticipantRepository participants;
    private final GameResultRepository results;
    private final SessionAccessService access;

    public RankingService(ParticipantRepository participants, GameResultRepository results, SessionAccessService access) {
        this.participants = participants;
        this.results = results;
        this.access = access;
    }

    /** Entrada mínima para calcular posiciones; independiente de JPA para poder probarla de forma aislada. */
    public record RankInput(Long participantId, Long userId, String displayName, int score, int correctAnswers) {
    }

    public record RankedEntry(RankInput input, int position, boolean tied) {
    }

    private static final Comparator<RankInput> ORDER = Comparator
            .comparingInt(RankInput::score).reversed()
            .thenComparing(Comparator.comparingInt(RankInput::correctAnswers).reversed())
            .thenComparing(RankInput::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(RankInput::participantId);

    /** Calcula posiciones con la regla de desempate documentada. Función pura. */
    public static List<RankedEntry> rank(List<RankInput> inputs) {
        List<RankInput> sorted = new ArrayList<>(inputs);
        sorted.sort(ORDER);
        int[] positions = new int[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0 && sameStanding(sorted.get(i), sorted.get(i - 1))) {
                positions[i] = positions[i - 1];
            } else {
                positions[i] = i + 1;
            }
        }
        List<RankedEntry> ranked = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            boolean tied = (i > 0 && positions[i - 1] == positions[i])
                    || (i + 1 < sorted.size() && positions[i + 1] == positions[i]);
            ranked.add(new RankedEntry(sorted.get(i), positions[i], tied));
        }
        return ranked;
    }

    private static boolean sameStanding(RankInput a, RankInput b) {
        return a.score() == b.score() && a.correctAnswers() == b.correctAnswers();
    }

    public static RankInput toInput(SessionParticipant p) {
        return new RankInput(p.getId(), p.getUser().getId(), p.getDisplayName(), p.getScore(), p.getCorrectAnswers());
    }

    /** Clasificación provisional calculada a partir de las puntuaciones actuales. */
    @Transactional(readOnly = true)
    public List<RankedEntry> provisional(Long sessionId) {
        return rank(participants.findBySessionIdOrderByJoinedAtAsc(sessionId).stream()
                .map(RankingService::toInput).toList());
    }

    /**
     * Clasificación de la sesión para quien consulta. Si la sesión terminó se lee de los resultados
     * definitivos persistidos; si no, es provisional.
     */
    @Transactional(readOnly = true)
    public RankingDTO rankingFor(Long sessionId, AuthUser viewer) {
        GameSession session = access.requireViewer(sessionId, viewer).session();
        List<ParticipantRankingDTO> entries = session.isFinished()
                ? finalEntries(sessionId, viewer.id())
                : provisional(sessionId).stream().map(e -> RankingMapper.toDto(e, viewer.id())).toList();
        return new RankingDTO(sessionId, session.getStatus(), !session.isFinished(), TIE_BREAK_RULE, entries);
    }

    @Transactional(readOnly = true)
    public List<ParticipantRankingDTO> finalEntries(Long sessionId, Long viewerUserId) {
        List<GameResult> rows = results.findOrderedBySession(sessionId);
        List<ParticipantRankingDTO> out = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            GameResult r = rows.get(i);
            boolean tied = (i > 0 && rows.get(i - 1).getPosition() == r.getPosition())
                    || (i + 1 < rows.size() && rows.get(i + 1).getPosition() == r.getPosition());
            out.add(RankingMapper.toDto(r, viewerUserId, tied));
        }
        return out;
    }

    /** Agrupa las posiciones 1 a 3 en puestos de podio. */
    public static PodiumDTO podium(Long sessionId, List<ParticipantRankingDTO> ranking) {
        List<PodiumDTO.PodiumPlaceDTO> places = new ArrayList<>();
        for (ParticipantRankingDTO row : ranking) {
            if (row.position() > 3) {
                break;
            }
            PodiumDTO.PodiumPlaceDTO last = places.isEmpty() ? null : places.get(places.size() - 1);
            if (last != null && last.position() == row.position()) {
                last.participants().add(row);
            } else {
                List<ParticipantRankingDTO> group = new ArrayList<>();
                group.add(row);
                places.add(new PodiumDTO.PodiumPlaceDTO(row.position(), row.score(), group));
            }
        }
        List<PodiumDTO.PodiumPlaceDTO> frozen = places.stream()
                .map(p -> new PodiumDTO.PodiumPlaceDTO(p.position(), p.score(), List.copyOf(p.participants())))
                .toList();
        return new PodiumDTO(sessionId, frozen);
    }
}
