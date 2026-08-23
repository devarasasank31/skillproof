package com.skillproof.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {
    List<JobSkill> findByJobId(Long jobId);

    @Query("""
        SELECT COUNT(DISTINCT js.job.id)
        FROM JobSkill js
        WHERE js.job.user.id = :userId AND js.skill.id = :skillId
        """)
    long countJobsRequiring(@Param("userId") Long userId, @Param("skillId") Long skillId);

    @Query("""
        SELECT js.matchedName AS name, COUNT(DISTINCT js.job.id) AS cnt
        FROM JobSkill js
        WHERE js.job.user.id = :userId
        GROUP BY js.matchedName
        ORDER BY cnt DESC
        """)
    List<NameCount> frequencyByUserRaw(@Param("userId") Long userId);

    interface NameCount {
        String getName();
        long getCnt();
    }

    default Map<String, Long> frequencyByUser(Long userId) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (NameCount nc : frequencyByUserRaw(userId)) {
            out.put(nc.getName(), nc.getCnt());
        }
        return out;
    }
}
