package com.skillproof.review;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndStatusNot(Long id, Review.Status status);

    @EntityGraph(attributePaths = {"userSkill", "userSkill.skill"})
    List<Review> findByUserSkill_User_IdAndStatusAndDueAtLessThanEqualOrderByDueAtAsc(
            Long userId, Review.Status status, Instant now);

    List<Review> findByUserSkillIdOrderByDueAtDesc(Long userSkillId);

    boolean existsByUserSkillIdAndStatus(Long userSkillId, Review.Status status);
}
