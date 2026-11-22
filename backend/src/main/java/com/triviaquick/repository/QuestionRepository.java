package com.triviaquick.repository;

import com.triviaquick.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("select q from Question q left join fetch q.answers where q.trivia.id = :triviaId and q.position = :position")
    Optional<Question> findWithAnswersByTriviaAndPosition(@Param("triviaId") Long triviaId, @Param("position") int position);

    long countByTriviaId(Long triviaId);
}
