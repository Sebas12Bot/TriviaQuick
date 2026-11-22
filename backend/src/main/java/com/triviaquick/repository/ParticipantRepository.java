package com.triviaquick.repository;

import com.triviaquick.model.SessionParticipant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<SessionParticipant, Long> {

    Optional<SessionParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);

    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);

    List<SessionParticipant> findBySessionIdOrderByJoinedAtAsc(Long sessionId);

    long countBySessionId(Long sessionId);

    List<SessionParticipant> findByUserId(Long userId);

    /** Bloqueo de fila para modificar la puntuación de un participante. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SessionParticipant p where p.id = :id")
    Optional<SessionParticipant> findByIdForUpdate(@Param("id") Long id);

    /** Bloquea varios participantes siempre en orden de id ascendente para evitar interbloqueos. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SessionParticipant p where p.id in :ids order by p.id asc")
    List<SessionParticipant> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}
