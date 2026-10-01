package com.skillproof.scoring;

import com.skillproof.skill.SkillScore;
import com.skillproof.skill.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillScoreRepository extends JpaRepository<SkillScore, Long> {
    java.util.List<SkillScore> findByUserSkill_User_IdAndUserSkill_Skill_IdOrderBySnapshotAtAsc(Long userId, Long skillId);

    Optional<SkillScore> findFirstByUserSkill_IdOrderBySnapshotAtDesc(Long userSkillId);

    /** Single query returning every snapshot for a user, so trend building avoids N+1 lookups. */
    @Query("SELECT s FROM SkillScore s JOIN FETCH s.userSkill us WHERE us.user.id = :userId ORDER BY s.snapshotAt ASC")
    List<SkillScore> findAllForUser(@Param("userId") Long userId);
}