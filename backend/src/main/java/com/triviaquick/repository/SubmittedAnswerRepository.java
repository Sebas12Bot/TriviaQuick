package com.triviaquick.repository;

import com.triviaquick.model.SubmittedAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmittedAnswerRepository extends JpaRepository<SubmittedAnswer, Long> {

    boolean existsByParticipantIdAndQuestionId(Long participantId, Long questionId);

    Optional<SubmittedAnswer> findByParticipantIdAndQuestionId(Long participantId, Long questionId);

    long countByQuestionIdAndParticipantSessionId(Long questionId, Long sessionId);
}
