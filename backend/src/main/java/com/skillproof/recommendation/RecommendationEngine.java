package com.skillproof.recommendation;

import com.skillproof.review.Review;
import com.skillproof.review.ReviewRepository;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.skill.SkillRepository;
import com.skillproof.skill.UserSkill;
import com.skillproof.skill.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RecommendationEngine {

    private final UserSkillRepository userSkills;
    private final ReviewRepository reviews;
    private final RecommendationRepository recommendations;
    private final RecalculationService recalculation;
    private final SkillRepository skills;

    public RecommendationEngine(UserSkillRepository userSkills, ReviewRepository reviews,
                                RecommendationRepository recommendations, RecalculationService recalculation,
                                SkillRepository skills) {
        this.userSkills = userSkills;
        this.reviews = reviews;
        this.recommendations = recommendations;
        this.recalculation = recalculation;
        this.skills = skills;
    }

    public record RecommendationDto(Long id, String actionType, String title, String reason,
                                    int priority, Integer effortMinutes, String skillName, String status) {}

    @Transactional
    public List<RecommendationDto> generateFor(Long userId) {
        recalculation.recalculateUser(userId);
        List<UserSkill> all = userSkills.findByUserIdOrderByConfidenceDesc(userId);
        List<Recommendation> out = new ArrayList<>();
        Instant now = Instant.now();

        List<Review> due = reviews.findByUserSkill_User_IdAndStatusAndDueAtLessThanEqualOrderByDueAtAsc(
                userId, Review.Status.DUE, now);
        for (int i = 0; i < Math.min(2, due.size()); i++) {
            var r = due.get(i);
            out.add(build(userId, "REVIEW",
                    "Do a retrieval test on " + r.getUserSkill().getSkill().getName(),
                    "A review is due today. Retention is at "
                            + Math.round(r.getUserSkill().getRetention() * 100) + "%. Testing yourself now is the cheapest way to keep it.",
                    90 - i, 5, r.getUserSkill()));
        }

        for (UserSkill us : all) {
            String name = us.getSkill().getName();
            switch (us.getState()) {
                case OVERCLAIMED -> out.add(build(userId, "UPDATE_RESUME",
                        "Reassess your claim of " + name,
                        "You currently claim " + name + ", but available evidence is low-evidence (knowledge "
                                + us.getKnowledgeScore() + "%, no practical proof yet). Either build proof or remove it from your resume until you can defend it.",
                        95, 30, us));
                case STALE -> out.add(build(userId, "PRACTICE",
                        "Refresh " + name,
                        "Predicted retention dropped to " + Math.round(us.getRetention() * 100)
                                + "% and it has been a while since you used it. A focused refresher prevents further decay.",
                        80, 20, us));
                case WEAK -> {
                    if (us.getMarketScore() >= 50) {
                        out.add(build(userId, "LEARN", "Strengthen " + name,
                                "Confidence is " + us.getConfidence() + "% but this skill appears in "
                                        + us.getMarketScore() + "% of your saved jobs - high market demand meets personal weakness here.",
                                75, 45, us));
                    } else {
                        out.add(build(userId, "LEARN", "Strengthen " + name,
                                "Confidence is only " + us.getConfidence() + "%.",
                                55, 30, us));
                    }
                }
                case AT_RISK -> out.add(build(userId, "PRACTICE", "Practice " + name,
                        "Retention or recent usage is slipping (confidence " + us.getConfidence()
                                + "%). One practical challenge will restore evidence.",
                        65, 30, us));
                case MASTERED, STRONG -> {
                    if (us.getActivityScore() <= 35) {
                        out.add(build(userId, "BUILD", "Build something small with " + name,
                                "You are strong in " + name + " (" + us.getConfidence()
                                        + "%) but have not used it recently. A small project keeps activity evidence alive.",
                                40, 45, us));
                    } else {
                        out.add(build(userId, "INTERVIEW", "Mock-interview yourself on " + name,
                                "Strong skills decay silently under interview pressure. Keep readiness high.",
                                30, 20, us));
                    }
                }
                default -> { }
            }
        }

        if (out.isEmpty()) {
            out.add(build(userId, "LEARN", "Claim your first skill to get started",
                    "Add the technologies you work with, then take a short assessment to calibrate your profile.",
                    50, 10, null));
        }

        out.sort(Comparator.comparingInt(Recommendation::getPriority).reversed());
        return out.stream().limit(8)
                .map(r -> new RecommendationDto(r.getId(), r.getActionType(), r.getTitle(),
                        r.getReason(), r.getPriority(), r.getEffortMinutes(), skillName(r.getSkillId()),
                        r.getStatus()))
                .toList();
    }

    private String skillName(Long skillId) {
        if (skillId == null) return null;
        return skills.findById(skillId).map(com.skillproof.skill.Skill::getName).orElse(null);
    }

    public List<Recommendation> open(Long userId) {
        return recommendations.findByUserIdAndStatusOrderByPriorityDesc(userId, "OPEN");
    }

    @Transactional
    public void refreshDaily(Long userId) {
        recommendations.resetTodayGenerated(userId, LocalDate.now());
        generateFor(userId);
    }

    private Recommendation build(Long userId, String type, String title, String reason,
                                 int priority, int effort, UserSkill us) {
        Recommendation r = new Recommendation();
        var u = new com.skillproof.user.User();
        u.setId(userId);
        r.setUser(u);
        r.setActionType(type);
        r.setTitle(title);
        r.setReason(reason);
        r.setPriority(priority);
        r.setEffortMinutes(effort);
        if (us != null) r.setSkillId(us.getSkill().getId());
        return recommendations.save(r);
    }
}
