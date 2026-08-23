package com.skillproof.analytics;

import com.skillproof.scoring.SkillScoreRepository;
import com.skillproof.security.CurrentUserId;
import com.skillproof.skill.SkillService;
import com.skillproof.skill.UserSkillRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class AnalyticsController {

    public record CategoryRow(String category, int avgConfidence, int count) {}
    public record TrendPoint(Instant snapshotAt, int confidence) {}
    public record SkillTrend(String skillName, List<TrendPoint> points) {}
    public record AnalyticsResponse(List<CategoryRow> categories, List<SkillTrend> topSkillTrends,
                                    int readiness, int strongest, String weakestName, int weakestConfidence) {}

    private final SkillService skills;
    private final UserSkillRepository userSkills;
    private final SkillScoreRepository snapshots;

    public AnalyticsController(SkillService skills, UserSkillRepository userSkills,
                               SkillScoreRepository snapshots) {
        this.skills = skills;
        this.userSkills = userSkills;
        this.snapshots = snapshots;
    }

    @GetMapping("/api/analytics")
    public AnalyticsResponse analytics(@CurrentUserId Long userId) {
        List<SkillService.SkillRow> rows = skills.listForUser(userId);

        Map<String, IntSummaryStatistics> byCategory = new LinkedHashMap<>();
        for (SkillService.SkillRow r : rows) {
            byCategory.computeIfAbsent(r.category() == null ? "General" : r.category(),
                            k -> new IntSummaryStatistics())
                    .accept(r.confidence());
        }
        List<CategoryRow> categories = byCategory.entrySet().stream()
                .map(e -> new CategoryRow(e.getKey(),
                        (int) Math.round(e.getValue().getAverage()),
                        (int) e.getValue().getCount()))
                .sorted((a, b) -> Integer.compare(b.avgConfidence(), a.avgConfidence()))
                .toList();

        List<SkillTrend> trends = new ArrayList<>();
        for (SkillService.SkillRow r : rows.stream().limit(4).toList()) {
            var usOpt = userSkills.findByUserIdAndSkillId(userId, r.skillId());
            if (usOpt.isEmpty()) continue;
            List<TrendPoint> pts = snapshots
                    .findByUserSkill_User_IdAndUserSkill_Skill_IdOrderBySnapshotAtAsc(userId, r.skillId())
                    .stream()
                    .map(s -> new TrendPoint(s.getSnapshotAt(), s.getConfidence()))
                    .collect(Collectors.toList());
            if (pts.isEmpty()) {
                pts.add(new TrendPoint(Instant.now(), r.confidence()));
            }
            trends.add(new SkillTrend(r.name(), pts));
        }

        int readiness = rows.isEmpty() ? 0
                : (int) Math.round(rows.stream().mapToInt(SkillService.SkillRow::confidence)
                        .average().orElse(0));
        int strongest = rows.stream().mapToInt(SkillService.SkillRow::confidence).max().orElse(0);
        SkillService.SkillRow weakest = rows.stream()
                .min(Comparator.comparingInt(SkillService.SkillRow::confidence))
                .orElse(null);

        return new AnalyticsResponse(categories, trends, readiness, strongest,
                weakest == null ? null : weakest.name(),
                weakest == null ? 0 : weakest.confidence());
    }
}
