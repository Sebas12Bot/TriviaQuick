package com.triviaquick.mapper;

import com.triviaquick.dto.ParticipantRankingDTO;
import com.triviaquick.model.GameResult;
import com.triviaquick.model.SessionParticipant;
import com.triviaquick.service.RankingService;

import java.util.Locale;

/** Mapeo explícito entre el modelo de clasificación y sus DTO. */
public final class RankingMapper {

    private RankingMapper() {
    }

    public static ParticipantRankingDTO toDto(RankingService.RankedEntry entry, Long viewerUserId) {
        RankingService.RankInput in = entry.input();
        return new ParticipantRankingDTO(in.participantId(), entry.position(), in.displayName(),
                initials(in.displayName()), in.score(), in.correctAnswers(),
                in.userId().equals(viewerUserId), entry.tied());
    }

    public static ParticipantRankingDTO toDto(GameResult result, Long viewerUserId, boolean tied) {
        SessionParticipant p = result.getParticipant();
        return new ParticipantRankingDTO(p.getId(), result.getPosition(), p.getDisplayName(),
                initials(p.getDisplayName()), result.getFinalScore(), result.getCorrectAnswers(),
                p.getUser().getId().equals(viewerUserId), tied);
    }

    /** Hasta dos iniciales en mayúscula: "Ana María López" → "AM". */
    public static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty() && Character.isLetterOrDigit(part.codePointAt(0))) {
                sb.appendCodePoint(part.codePointAt(0));
            }
            if (sb.codePointCount(0, sb.length()) == 2) {
                break;
            }
        }
        return sb.isEmpty() ? "?" : sb.toString().toUpperCase(Locale.ROOT);
    }
}
