package com.triviaquick.mapper;

import com.triviaquick.dto.GameEventDTO;
import com.triviaquick.dto.PowerUpDTO;
import com.triviaquick.model.GameEvent;
import com.triviaquick.model.PlayerPowerUp;
import com.triviaquick.service.ChallengeBank;

import java.util.ArrayList;
import java.util.List;

public final class EventMapper {

    private EventMapper() {
    }

    public static PowerUpDTO toDto(PlayerPowerUp p) {
        return new PowerUpDTO(p.getId(), p.getType(), p.getStatus(), p.getGrantedAt());
    }

    /** El reto se expone sin su respuesta correcta. */
    public static GameEventDTO toDto(GameEvent e, ChallengeBank.Challenge challenge, boolean eligible, boolean responded,
                                     Boolean myResponseCorrect, long responsesCount) {
        List<String> duelists = new ArrayList<>();
        if (e.getDuelFirst() != null) {
            duelists.add(e.getDuelFirst().getDisplayName());
        }
        if (e.getDuelSecond() != null) {
            duelists.add(e.getDuelSecond().getDisplayName());
        }
        GameEventDTO.ChallengeDTO c = challenge == null ? null
                : new GameEventDTO.ChallengeDTO(challenge.category(), challenge.prompt(), challenge.options());
        return new GameEventDTO(e.getId(), e.getType(), e.getStatus(), e.getAfterQuestionIndex(), e.getStartedAt(),
                e.getExpiresAt(), c, eligible, responded, myResponseCorrect, duelists, responsesCount, e.getOutcome());
    }
}
