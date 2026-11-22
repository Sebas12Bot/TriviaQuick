package com.triviaquick.repository;

import com.triviaquick.model.Trivia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TriviaRepository extends JpaRepository<Trivia, Long> {

    List<Trivia> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
