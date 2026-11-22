package com.triviaquick.repository;

import com.triviaquick.model.GameSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    Optional<GameSession> findByJoinCode(String joinCode);

    boolean existsByJoinCode(String joinCode);

    long countByTriviaId(Long triviaId);

    boolean existsByTriviaIdAndStatusNot(Long triviaId, com.triviaquick.model.SessionStatus status);

    long countByHostIdAndStatus(Long hostId, com.triviaquick.model.SessionStatus status);

    @Query("""
            select s from GameSession s join fetch s.trivia
            where s.host.id = :hostId and s.status = com.triviaquick.model.SessionStatus.FINISHED
            order by s.finishedAt desc
            """)
    List<GameSession> findRecentFinishedByHost(@Param("hostId") Long hostId, org.springframework.data.domain.Pageable page);

    /** Bloqueo exclusivo (SELECT ... FOR UPDATE) para las transiciones de estado de la partida. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GameSession s where s.id = :id")
    Optional<GameSession> findByIdForUpdate(@Param("id") Long id);

    /**
     * Bloqueo compartido (SELECT ... FOR SHARE): varias respuestas se procesan en paralelo,
     * pero una transición (avance o cierre) espera a que terminen y ninguna entra después.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from GameSession s where s.id = :id")
    Optional<GameSession> findByIdForShare(@Param("id") Long id);

    @Query("""
            select s from GameSession s join fetch s.trivia
            where s.host.id = :userId
               or exists (select 1 from SessionParticipant p where p.session = s and p.user.id = :userId)
            order by s.createdAt desc
            """)
    List<GameSession> findVisibleTo(@Param("userId") Long userId);
}
