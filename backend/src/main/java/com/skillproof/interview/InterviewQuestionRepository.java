package com.skillproof.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findBySessionIdOrderByOrderIndexAsc(Long sessionId);

    Optional<InterviewQuestion> findByIdAndSession_Id(Long id, Long sessionId);
}
