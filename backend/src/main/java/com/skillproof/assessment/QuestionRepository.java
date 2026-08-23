package com.skillproof.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByAssessmentIdOrderByOrderIndexAsc(Long assessmentId);
}
