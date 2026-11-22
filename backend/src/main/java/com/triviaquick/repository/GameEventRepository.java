package com.triviaquick.repository;

import com.triviaquick.model.GameEvent;
import com.triviaquick.model.GameEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    Optional<GameEvent> findFirstBySessionIdAndStatusOrderByStartedAtDesc(Long sessionId, GameEventStatus status);

    Optional<GameEvent> findFirstBySessionIdOrderByStartedAtDescIdDesc(Long sessionId);

    long countBySessionId(Long sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from GameEvent e where e.id = :id")
    Optional<GameEvent> findByIdForUpdate(@Param("id") Long id);
}
