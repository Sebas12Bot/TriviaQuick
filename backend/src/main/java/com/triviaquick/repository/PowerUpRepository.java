package com.triviaquick.repository;

import com.triviaquick.model.PlayerPowerUp;
import com.triviaquick.model.PowerUpStatus;
import com.triviaquick.model.PowerUpType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PowerUpRepository extends JpaRepository<PlayerPowerUp, Long> {

    List<PlayerPowerUp> findByParticipantIdOrderByGrantedAtAscIdAsc(Long participantId);

    /** Habilidades de un tipo y estado, bloqueadas para consumirlas una sola vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from PlayerPowerUp p
            where p.participant.id = :participantId and p.type = :type and p.status = :status
            order by p.grantedAt asc, p.id asc
            """)
    List<PlayerPowerUp> findForUpdate(@Param("participantId") Long participantId,
                                      @Param("type") PowerUpType type,
                                      @Param("status") PowerUpStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlayerPowerUp p where p.id = :id")
    Optional<PlayerPowerUp> findByIdForUpdate(@Param("id") Long id);
}
