package com.triviaquick.mapper;

import com.triviaquick.dto.ScoreTransactionDTO;
import com.triviaquick.model.ScoreTransaction;

public final class ScoreTransactionMapper {

    private ScoreTransactionMapper() {
    }

    public static ScoreTransactionDTO toDto(ScoreTransaction t) {
        return new ScoreTransactionDTO(t.getId(), t.getParticipant().getId(), t.getParticipant().getDisplayName(),
                t.getDelta(), t.getBalanceAfter(), t.getReason(), t.getDescription(), t.getCreatedAt());
    }
}
