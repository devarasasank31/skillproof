package com.skillproof.scoring;

import com.skillproof.skill.SkillScore;
import com.skillproof.skill.UserSkill;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillScoreRepository extends JpaRepository<SkillScore, Long> {
    java.util.List<SkillScore> findByUserSkill_User_IdAndUserSkill_Skill_IdOrderBySnapshotAtAsc(Long userId, Long skillId);

    Optional<SkillScore> findFirstByUserSkill_IdOrderBySnapshotAtDesc(Long userSkillId);

    /**
     * Recent snapshots for one user, newest first and capped by the caller. Trend charts only
     * need the last handful of points per skill, so loading the entire history is wasted work.
     */
    @Query("SELECT s FROM SkillScore s JOIN FETCH s.userSkill us WHERE us.user.id = :userId ORDER BY s.snapshotAt DESC")
    List<SkillScore> findRecentForUser(@Param("userId") Long userId, Pageable pageable);

    /** Recent snapshots grouped per skill id, oldest-first within each group. */
    default java.util.Map<Long, List<SkillScore>> recentBySkill(Long userId, int limit) {
        java.util.Map<Long, List<SkillScore>> bySkill = new java.util.LinkedHashMap<>();
        for (SkillScore s : findRecentForUser(userId, org.springframework.data.domain.PageRequest.of(0, limit))) {
            bySkill.computeIfAbsent(s.getUserSkill().getSkill().getId(), k -> new java.util.ArrayList<>())
                    .add(s);
        }
        bySkill.values().forEach(l -> java.util.Collections.reverse(l));
        return bySkill;
    }
}