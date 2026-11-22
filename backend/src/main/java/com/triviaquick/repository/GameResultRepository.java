package com.triviaquick.repository;

import com.triviaquick.model.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    @Query("""
            select r from GameResult r join fetch r.participant p join fetch p.user
            where r.session.id = :sessionId
            order by r.position asc, p.displayName asc
            """)
    List<GameResult> findOrderedBySession(@Param("sessionId") Long sessionId);

    boolean existsBySessionId(Long sessionId);
}
