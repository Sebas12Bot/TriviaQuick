package com.triviaquick.dto;

public record EventResponseResultDTO(boolean correct, int pointsAwarded, int totalScore, String message) {
}
