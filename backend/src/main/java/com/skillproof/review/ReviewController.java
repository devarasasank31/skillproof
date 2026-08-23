package com.skillproof.review;

import com.skillproof.security.CurrentUserId;
import com.skillproof.skill.UserSkill;
import com.skillproof.skill.UserSkillRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    public record DueReview(Long reviewId, Long skillId, String skillName, Instant dueAt) {}
    public record CompleteRequest(@Min(0) @Max(100) int score) {}
    public record CompletedReview(Long reviewId, String status, Integer score, Integer intervalDays, Instant nextReviewAt) {}

    private final ReviewService reviews;
    private final UserSkillRepository userSkills;

    public ReviewController(ReviewService reviews, UserSkillRepository userSkills) {
        this.reviews = reviews;
        this.userSkills = userSkills;
    }

    @GetMapping("/today")
    public List<DueReview> today(@CurrentUserId Long userId) {
        return reviews.dueToday(userId).stream()
                .map(r -> new DueReview(r.getId(), r.getUserSkill().getSkill().getId(),
                        r.getUserSkill().getSkill().getName(), r.getDueAt()))
                .toList();
    }

    @PostMapping("/{id}/complete")
    public CompletedReview complete(@CurrentUserId Long userId, @PathVariable Long id,
                                    @jakarta.validation.Valid @RequestBody CompleteRequest body) {
        Review r = reviews.completeReview(userId, id, body.score());
        UserSkill us = r.getUserSkill();
        return new CompletedReview(r.getId(), r.getStatus().name(), r.getScore(), r.getIntervalDays(),
                us.getNextReviewAt());
    }
}

