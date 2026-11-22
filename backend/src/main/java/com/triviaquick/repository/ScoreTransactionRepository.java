package com.triviaquick.repository;

import com.triviaquick.model.ScoreTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScoreTransactionRepository extends JpaRepository<ScoreTransaction, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<ScoreTransaction> findByParticipantIdOrderByCreatedAtAscIdAsc(Long participantId);

    List<ScoreTransaction> findBySessionIdOrderByCreatedAtAscIdAsc(Long sessionId);

    @Query("select coalesce(sum(t.delta), 0) from ScoreTransaction t where t.participant.id = :participantId")
    long sumDeltaByParticipant(@Param("participantId") Long participantId);
}
