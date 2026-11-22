package com.triviaquick.repository;

import com.triviaquick.model.GameEventResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameEventResponseRepository extends JpaRepository<GameEventResponse, Long> {

    boolean existsByEventIdAndParticipantId(Long eventId, Long participantId);

    Optional<GameEventResponse> findByEventIdAndParticipantId(Long eventId, Long participantId);

    List<GameEventResponse> findByEventId(Long eventId);

    long countByEventId(Long eventId);
}
