package com.skillproof.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Assessment> findByUserIdAndSkillIdOrderByCreatedAtDesc(Long userId, Long skillId);
    Optional<Assessment> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(avg(a.score), 0) from Assessment a where a.user.id = :userId and a.skill.id = :skillId and a.status = 'COMPLETED' and a.score is not null")
    double averageScore(@Param("userId") Long userId, @Param("skillId") Long skillId);

    long countByUserIdAndSkillIdAndStatus(Long userId, Long skillId, Assessment.Status status);

    long countByUserId(Long userId);
}
