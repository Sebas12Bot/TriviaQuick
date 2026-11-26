package com.triviaquick.repository;

import com.triviaquick.model.SubmittedAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmittedAnswerRepository extends JpaRepository<SubmittedAnswer, Long> {

    boolean existsByParticipantIdAndQuestionId(Long participantId, Long questionId);

    Optional<SubmittedAnswer> findByParticipantIdAndQuestionId(Long participantId, Long questionId);

    long countByQuestionIdAndParticipantSessionId(Long questionId, Long sessionId);

    /** Respuestas correctas de una pregunta en una sesión, de la más rápida a la más lenta (desempate por llegada). */
    @org.springframework.data.jpa.repository.Query("""
            select a from SubmittedAnswer a join fetch a.participant p
            where a.question.id = :questionId and p.session.id = :sessionId and a.correct = true
            order by a.responseTimeMs asc, a.submittedAt asc, a.id asc
            """)
    java.util.List<SubmittedAnswer> findCorrectFastestFirst(
            @org.springframework.data.repository.query.Param("questionId") Long questionId,
            @org.springframework.data.repository.query.Param("sessionId") Long sessionId,
            org.springframework.data.domain.Pageable page);

    /** Todas las respuestas de un participante, con pregunta y opción elegida. */
    @org.springframework.data.jpa.repository.Query("""
            select a from SubmittedAnswer a join fetch a.question join fetch a.answer
            where a.participant.id = :participantId
            """)
    java.util.List<SubmittedAnswer> findWithDetailsByParticipant(
            @org.springframework.data.repository.query.Param("participantId") Long participantId);
}
