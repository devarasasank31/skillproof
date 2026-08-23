package com.skillproof.review;

import com.skillproof.exception.ApiException;
import com.skillproof.evidence.KnowledgeEvent;
import com.skillproof.evidence.KnowledgeEventRepository;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.skill.UserSkill;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final KnowledgeEventRepository knowledgeEvents;
    private final DecayEngine decayEngine;
    private final RecalculationService recalculation;

    public ReviewService(ReviewRepository reviews, KnowledgeEventRepository knowledgeEvents,
                         DecayEngine decayEngine, RecalculationService recalculation) {
        this.reviews = reviews;
        this.knowledgeEvents = knowledgeEvents;
        this.decayEngine = decayEngine;
        this.recalculation = recalculation;
    }

    public List<Review> dueToday(Long userId) {
        return reviews.findByUserSkill_User_IdAndStatusAndDueAtLessThanEqualOrderByDueAtAsc(
                userId, Review.Status.DUE, Instant.now());
    }

    @Transactional
    public Review completeReview(Long userId, Long reviewId, int score) {
        Review review = reviews.findByIdAndStatusNot(reviewId, Review.Status.COMPLETED)
                .orElseThrow(() -> ApiException.notFound("Review not found or already completed"));
        UserSkill us = review.getUserSkill();
        if (!us.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("Not your review");
        }
        if (score < 0 || score > 100) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Score must be 0-100");
        }
        double retentionBefore = us.getRetention();
        review.setScore(score);
        review.setIntervalDays(decayEngine.intervalDaysForScore(score));
        review.setCompletedAt(Instant.now());
        review.setStatus(Review.Status.COMPLETED);
        review.setMemoryStrength(us.getMemoryStrength());
        review.setRetentionBefore(retentionBefore);

        decayEngine.applyReview(us, score);

        KnowledgeEvent event = new KnowledgeEvent();
        event.setUserSkill(us);
        event.setInitialRetention(retentionBefore);
        event.setReviewScore(score);
        event.setMemoryStrength(us.getMemoryStrength());
        event.setElapsedDays((int) Math.max(0,
                java.time.Duration.between(review.getCreatedAt(), review.getCompletedAt()).toDays()));
        event.setPredictedRetention(us.getRetention());
        event.setReviewedAt(review.getCompletedAt());
        event.setNextReviewAt(us.getNextReviewAt());
        knowledgeEvents.save(event);

        reviews.save(review);
        recalculation.recalculateUserSkill(us);
        return review;
    }
}
