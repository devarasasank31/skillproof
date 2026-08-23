package com.skillproof.dashboard;

import com.skillproof.recommendation.Recommendation;
import com.skillproof.recommendation.RecommendationEngine;
import com.skillproof.review.Review;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.security.CurrentUserId;
import com.skillproof.skill.Skill;
import com.skillproof.skill.SkillRepository;

import com.skillproof.skill.SkillService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
public class DashboardController {

    public record TrendSkill(Long id, String name, int confidence, String state, String trend) {}
    public record NbaDto(String actionType, String title, String reason, Integer effortMinutes, String skillName) {}
    public record DueReviewDto(Long reviewId, Long skillId, String skillName) {}
    public record DashboardResponse(String greeting, LocalDate date, int readiness, int retentionAvg,
                                    List<TrendSkill> atRisk, NbaDto nextBestAction,
                                    long totalSkills, long dueReviews, long openRecommendations,
                                    List<DueReviewDto> dueToday) {}

    private final SkillService skills;
    private final SkillRepository skillCatalog;
    private final com.skillproof.scoring.SkillScoreRepository snapshots;
    private final com.skillproof.review.ReviewRepository reviews;
    private final com.skillproof.recommendation.RecommendationRepository recs;
    private final RecommendationEngine engine;
    private final RecalculationService recalculation;

    public DashboardController(SkillService skills, SkillRepository skillCatalog,
                               com.skillproof.scoring.SkillScoreRepository snapshots,
                               com.skillproof.review.ReviewRepository reviews,
                               com.skillproof.recommendation.RecommendationRepository recs,
                               RecommendationEngine engine, RecalculationService recalculation) {
        this.skills = skills;
        this.skillCatalog = skillCatalog;
        this.snapshots = snapshots;
        this.reviews = reviews;
        this.recs = recs;
        this.engine = engine;
        this.recalculation = recalculation;
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard(@CurrentUserId Long userId) {
        recalculation.recalculateUser(userId);
        List<SkillService.SkillRow> all = skills.listForUser(userId);

        int readiness = all.isEmpty() ? 0
                : (int) Math.round(all.stream().mapToInt(SkillService.SkillRow::confidence).average().orElse(0));
        int retentionAvg = all.isEmpty() ? 100
                : (int) Math.round(all.stream().mapToDouble(SkillService.SkillRow::retention).average().orElse(1.0) * 100);

        List<TrendSkill> atRisk = all.stream()
                .filter(r -> r.state().equals("OVERCLAIMED") || r.state().equals("AT_RISK")
                        || r.state().equals("STALE") || r.state().equals("WEAK"))
                .limit(6)
                .map(r -> new TrendSkill(r.id(), r.name(), r.confidence(), r.state(), trendFor(userId, r)))
                .toList();

        List<Recommendation> open = recs.findByUserIdAndStatusOrderByPriorityDesc(userId, "OPEN");
        if (open.isEmpty() && !all.isEmpty()) {
            engine.generateFor(userId);
            open = recs.findByUserIdAndStatusOrderByPriorityDesc(userId, "OPEN");
        }
        NbaDto nba = open.isEmpty() ? null : toNba(open.get(0));

        List<Review> due = reviews.findByUserSkill_User_IdAndStatusAndDueAtLessThanEqualOrderByDueAtAsc(
                userId, Review.Status.DUE, Instant.now());
        List<DueReviewDto> dueDtos = due.stream()
                .limit(5)
                .map(r -> new DueReviewDto(r.getId(), r.getUserSkill().getSkill().getId(),
                        r.getUserSkill().getSkill().getName()))
                .toList();

        int hour = LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";

        return new DashboardResponse(greeting, LocalDate.now(), readiness, retentionAvg,
                atRisk, nba, all.size(), due.size(), open.size(), dueDtos);
    }

    private NbaDto toNba(Recommendation top) {
        String skillName = top.getSkillId() == null ? null
                : skillCatalog.findById(top.getSkillId()).map(Skill::getName).orElse(null);
        return new NbaDto(top.getActionType(), top.getTitle(), top.getReason(),
                top.getEffortMinutes(), skillName);
    }

    private String trendFor(Long userId, SkillService.SkillRow row) {
        var snaps = snapshots.findByUserSkill_User_IdAndUserSkill_Skill_IdOrderBySnapshotAtAsc(
                userId, row.skillId());
        if (snaps.size() < 2) return "flat";
        int last = snaps.get(snaps.size() - 1).getConfidence();
        int prev = snaps.get(Math.max(0, snaps.size() - 4)).getConfidence();
        if (last > prev) return "up";
        if (last < prev) return "down";
        return "flat";
    }
}

