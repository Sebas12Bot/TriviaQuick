package com.triviaquick.dto;

import com.triviaquick.model.PowerUpStatus;
import com.triviaquick.model.PowerUpType;

import java.time.Instant;

public record PowerUpDTO(Long id, PowerUpType type, PowerUpStatus status, Instant grantedAt) {
}
