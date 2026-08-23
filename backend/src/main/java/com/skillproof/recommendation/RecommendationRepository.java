package com.skillproof.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdAndStatusOrderByPriorityDesc(Long userId, String status);

    long countByUserIdAndStatus(Long userId, String status);

    @Modifying
    @Query("UPDATE Recommendation r SET r.status = 'DONE' WHERE r.user.id = :userId AND r.generatedOn < :today")
    void closeStale(@Param("userId") Long userId, @Param("today") LocalDate today);

    default void resetTodayGenerated(Long userId, LocalDate today) {
        closeStale(userId, today);
    }
}
