package com.skillproof.scoring;

import com.skillproof.skill.SkillScore;
import com.skillproof.skill.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillScoreRepository extends JpaRepository<SkillScore, Long> {
    java.util.List<SkillScore> findByUserSkill_User_IdAndUserSkill_Skill_IdOrderBySnapshotAtAsc(Long userId, Long skillId);
}
