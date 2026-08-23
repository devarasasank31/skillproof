package com.skillproof.recommendation;

import com.skillproof.security.CurrentUserId;
import com.skillproof.skill.SkillRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationEngine engine;
    private final RecommendationRepository recommendations;
    private final SkillRepository skills;

    public RecommendationController(RecommendationEngine engine, RecommendationRepository recommendations,
                                    SkillRepository skills) {
        this.engine = engine;
        this.recommendations = recommendations;
        this.skills = skills;
    }

    @GetMapping
    public List<RecommendationEngine.RecommendationDto> list(@CurrentUserId Long userId) {
        return recommendations.findByUserIdAndStatusOrderByPriorityDesc(userId, "OPEN").stream()
                .map(r -> new RecommendationEngine.RecommendationDto(r.getId(), r.getActionType(),
                        r.getTitle(), r.getReason(), r.getPriority(), r.getEffortMinutes(),
                        skillName(r.getSkillId()), r.getStatus()))
                .toList();
    }

    private String skillName(Long skillId) {
        if (skillId == null) return null;
        return skills.findById(skillId).map(s -> s.getName()).orElse(null);
    }
}
