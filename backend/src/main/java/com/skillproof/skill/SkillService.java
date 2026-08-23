package com.skillproof.skill;

import com.skillproof.evidence.SkillEvidence;
import com.skillproof.evidence.SkillEvidenceRepository;
import com.skillproof.exception.ApiException;
import com.skillproof.review.ReviewRepository;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.scoring.SkillScoreRepository;
import com.skillproof.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SkillService {

    private final SkillRepository skills;
    private final UserSkillRepository userSkills;
    private final UserRepository users;
    private final SkillEvidenceRepository evidence;
    private final RecalculationService recalculation;
    private final SkillScoreRepository scoreRepo;
    private final ReviewRepository reviewRepo;

    public SkillService(SkillRepository skills, UserSkillRepository userSkills, UserRepository users,
                        SkillEvidenceRepository evidence, RecalculationService recalculation,
                        SkillScoreRepository scoreRepo, ReviewRepository reviewRepo) {
        this.skills = skills;
        this.userSkills = userSkills;
        this.users = users;
        this.evidence = evidence;
        this.recalculation = recalculation;
        this.scoreRepo = scoreRepo;
        this.reviewRepo = reviewRepo;
    }

    public record SkillRow(Long id, Long skillId, String name, String category, String claimSource,
                           String state, int confidence, int knowledge, int practical, int activity,
                           int market, double retention, Instant nextReviewAt, Instant lastActivityAt) {}

    public record EvidenceRow(String type, String description, int points, Instant occurredAt) {}

    public record SnapshotRow(Instant snapshotAt, int confidence, int knowledge, int practical,
                              int activity, int market) {}

    public record ReviewRow(Long reviewId, Instant dueAt, String status, Integer score) {}

    public record SkillDetail(SkillRow skill, List<EvidenceRow> evidence, List<SnapshotRow> snapshots,
                              List<ReviewRow> reviews, double memoryStrength) {}

    public record CatalogItem(Long id, String name, String category) {}

    public record ClaimRequest(String skillName) {}

    public List<SkillRow> listForUser(Long userId) {
        return userSkills.findByUserIdOrderByConfidenceDesc(userId).stream().map(this::toRow).toList();
    }

    public SkillRow toRow(UserSkill us) {
        return new SkillRow(us.getId(), us.getSkill().getId(), us.getSkill().getName(),
                us.getSkill().getCategory(), us.getClaimSource(), us.getState().name(),
                us.getConfidence(), us.getKnowledgeScore(), us.getPracticalScore(), us.getActivityScore(),
                us.getMarketScore(), Math.round(us.getRetention() * 1000.0) / 1000.0,
                us.getNextReviewAt(), us.getLastActivityAt());
    }

    @Transactional
    public SkillRow claim(Long userId, ClaimRequest req) {
        String name = req.skillName() == null ? "" : req.skillName().trim();
        if (name.isEmpty() || name.length() > 140) {
            throw ApiException.badRequest("VALIDATION_ERROR", "Skill name must be 1-140 characters");
        }
        Skill skill = skills.findByNameIgnoreCase(name).orElseGet(() -> {
            Skill s = new Skill();
            s.setName(capitalize(name));
            s.setCategory("General");
            return skills.save(s);
        });
        if (userSkills.existsByUserIdAndSkillId(userId, skill.getId())) {
            throw ApiException.conflict("SKILL_EXISTS", "You already claimed " + skill.getName());
        }
        UserSkill us = new UserSkill();
        us.setUser(users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")));
        us.setSkill(skill);
        us.setClaimSource("MANUAL");
        userSkills.save(us);

        SkillEvidence ev = new SkillEvidence();
        ev.setUserSkill(us);
        ev.setUserId(userId);
        ev.setEvidenceType(SkillEvidence.Type.CLAIM);
        ev.setDescription("Claimed manually");
        ev.setPoints(0);
        evidence.save(ev);

        recalculation.recalculateUserSkill(us);
        return toRow(userSkills.findById(us.getId()).orElseThrow());
    }

    @Transactional
    public void unclaim(Long userId, Long id) {
        UserSkill us = userSkills.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Skill not found"));
        userSkills.delete(us);
    }

    @Transactional(readOnly = true)
    public SkillDetail detail(Long userId, Long id) {
        UserSkill us = userSkills.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Skill not found"));
        List<EvidenceRow> evs = evidence.findByUserSkillIdOrderByOccurredAtDesc(id).stream()
                .map(e -> new EvidenceRow(e.getEvidenceType().name(), e.getDescription(), e.getPoints(),
                        e.getOccurredAt()))
                .toList();
        List<SnapshotRow> snaps = snapshots(userId, us);
        List<ReviewRow> revs = reviewRepo.findByUserSkillIdOrderByDueAtDesc(id).stream()
                .map(r -> new ReviewRow(r.getId(), r.getDueAt(), r.getStatus().name(), r.getScore()))
                .toList();
        return new SkillDetail(toRow(us), evs, snaps, revs, us.getMemoryStrength());
    }

    private List<SnapshotRow> snapshots(Long userId, UserSkill us) {
        return scoreRepo.findByUserSkill_User_IdAndUserSkill_Skill_IdOrderBySnapshotAtAsc(
                        userId, us.getSkill().getId()).stream()
                .map(s -> new SnapshotRow(s.getSnapshotAt(), s.getConfidence(), s.getKnowledge(),
                        s.getPractical(), s.getActivity(), s.getMarket()))
                .toList();
    }

    public List<SkillRow> atRisk(Long userId) {
        return listForUser(userId).stream()
                .filter(r -> r.state().equals("OVERCLAIMED") || r.state().equals("AT_RISK")
                        || r.state().equals("STALE") || r.state().equals("WEAK"))
                .toList();
    }

    public List<CatalogItem> catalog(String q) {
        var page = Pageable.ofSize(20);
        List<Skill> found = (q == null || q.isBlank())
                ? skills.findAll()
                : skills.findByNameContainingIgnoreCaseOrderByNameAsc(q.trim(), page).getContent();
        return found.stream()
                .limit(20)
                .map(s -> new CatalogItem(s.getId(), s.getName(), s.getCategory()))
                .toList();
    }

    static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
