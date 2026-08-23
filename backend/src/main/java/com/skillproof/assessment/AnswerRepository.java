package com.skillproof.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    Optional<Answer> findByQuestionIdAndUserId(Long questionId, Long userId);
    List<Answer> findByUserIdOrderByCreatedAtDesc(Long userId);
}
