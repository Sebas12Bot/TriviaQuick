package com.triviaquick.dto;

import com.triviaquick.model.ScoreReason;

import java.time.Instant;

public record ScoreTransactionDTO(Long id, Long participantId, String participantName, int delta, int balanceAfter,
                                  ScoreReason reason, String description, Instant createdAt) {
}
