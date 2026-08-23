package com.skillproof.skill;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {

    @EntityGraph(attributePaths = {"skill"})
    List<UserSkill> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"skill"})
    Optional<UserSkill> findByIdAndUserId(Long id, Long userId);

    Optional<UserSkill> findByUserIdAndSkillId(Long userId, Long skillId);

    @Query("""
        SELECT us FROM UserSkill us JOIN FETCH us.skill s
        WHERE us.user.id = :userId AND lower(s.name) = lower(:skillName)
        """)
    Optional<UserSkill> findByUserIdAndSkillName(@Param("userId") Long userId, @Param("skillName") String skillName);

    @EntityGraph(attributePaths = {"skill"})
    List<UserSkill> findByUserIdOrderByConfidenceDesc(Long userId);

    boolean existsByUserIdAndSkillId(Long userId, Long skillId);

    long countByUserId(Long userId);
}
