package com.skillproof.challenge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChallengeSubmissionRepository extends JpaRepository<ChallengeSubmission, Long> {

    Optional<ChallengeSubmission> findTopByChallengeIdAndUserIdOrderByCreatedAtDesc(Long challengeId, Long userId);

    @Query(value = """
        SELECT COALESCE(LEAST(100, ROUND(AVG(best.score)))::int, 0)
        FROM (
            SELECT DISTINCT ON (cs.challenge_id) cs.score AS score
            FROM challenge_submissions cs
            JOIN practical_challenges pc ON pc.id = cs.challenge_id
            WHERE cs.user_id = :userId AND pc.skill_name = :skillName
            ORDER BY cs.challenge_id, cs.score DESC
        ) best
        """, nativeQuery = true)
    Integer avgBestSubmissionScore(@Param("userId") Long userId, @Param("skillName") String skillName);

    @Query(value = """
        SELECT COALESCE(LEAST(100, COUNT(DISTINCT cs.challenge_id) * 34)::int, 0)
        FROM challenge_submissions cs
        JOIN practical_challenges pc ON pc.id = cs.challenge_id
        WHERE cs.user_id = :userId AND pc.skill_name = :skillName AND cs.score >= 60
        """, nativeQuery = true)
    Integer practicalCoverage(@Param("userId") Long userId, @Param("skillName") String skillName);

    @Query("""
        SELECT s FROM ChallengeSubmission s WHERE s.userId = :userId
        ORDER BY s.createdAt DESC
        """)
    List<ChallengeSubmission> findRecent(@Param("userId") Long userId, org.springframework.data.domain.Pageable pageable);

    long countByUserId(Long userId);

    List<ChallengeSubmission> findByUserIdOrderByCreatedAtDesc(Long userId);
}
