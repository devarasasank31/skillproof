package com.skillproof.skill;

import com.skillproof.evidence.SkillEvidence;
import com.skillproof.evidence.SkillEvidenceRepository;
import com.skillproof.scoring.RecalculationService;
import com.skillproof.security.CurrentUserId;
import com.skillproof.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SkillsCollectionController {

    public record GraphNode(Long id, String name, String category) {}
    public record GraphEdge(Long from, Long to, String type) {}
    public record GraphResponse(List<GraphNode> nodes, List<GraphEdge> edges) {}
    public record EdgeRequest(@NotBlank String fromSkillName, @NotBlank String toSkillName,
                              @NotBlank String type) {}

    private final SkillService skills;
    private final UserSkillRepository userSkills;
    private final UserRepository users;
    private final SkillEdgeRepository edges;
    private final SkillEvidenceRepository evidence;
    private final RecalculationService recalculation;

    public SkillsCollectionController(SkillService skills, UserSkillRepository userSkills,
                                      UserRepository users, SkillEdgeRepository edges,
                                      SkillEvidenceRepository evidence, RecalculationService recalculation) {
        this.skills = skills;
        this.userSkills = userSkills;
        this.users = users;
        this.edges = edges;
        this.evidence = evidence;
        this.recalculation = recalculation;
    }

    @GetMapping("/api/skills")
    public List<SkillService.SkillRow> list(@CurrentUserId Long userId) {
        return skills.listForUser(userId);
    }

    @PostMapping("/api/skills")
    public SkillService.SkillRow claim(@CurrentUserId Long userId,
                                       @Valid @RequestBody SkillService.ClaimRequest req) {
        return skills.claim(userId, req);
    }

    @GetMapping("/api/graph")
    public GraphResponse graph(@CurrentUserId Long userId) {
        List<GraphNode> nodes = userSkills.findByUserIdOrderByConfidenceDesc(userId).stream()
                .map(us -> new GraphNode(us.getId(), us.getSkill().getName(), us.getSkill().getCategory()))
                .toList();
        List<GraphEdge> es = edges.findAll().stream()
                .map(e -> new GraphEdge(e.getFromSkillId(), e.getToSkillId(), e.getType()))
                .toList();
        return new GraphResponse(nodes, es);
    }

    @PostMapping("/api/graph/edges")
    public GraphEdge addEdge(@CurrentUserId Long userId, @Valid @RequestBody EdgeRequest req) {
        String type = req.type().toUpperCase();
        if (!List.of("PREREQUISITE", "RELATED", "PART_OF", "USED_WITH").contains(type)) {
            throw com.skillproof.exception.ApiException.badRequest("VALIDATION_ERROR",
                    "type must be PREREQUISITE, RELATED, PART_OF or USED_WITH");
        }
        var from = resolve(userId, req.fromSkillName());
        var to = resolve(userId, req.toSkillName());
        if (from.equals(to)) {
            throw com.skillproof.exception.ApiException.badRequest("VALIDATION_ERROR",
                    "Cannot link a skill to itself");
        }
        SkillEdge e = new SkillEdge();
        e.setFromSkillId(from);
        e.setToSkillId(to);
        e.setType(type);
        return edges.save(e) == null ? null : new GraphEdge(e.getFromSkillId(), e.getToSkillId(), e.getType());
    }

    private Long resolve(Long userId, String name) {
        return userSkills.findByUserIdAndSkillName(userId, name)
                .or(() -> userSkills.findByUserIdAndSkillName(userId, SkillService.capitalize(name.trim())))
                .map(UserSkill::getId)
                .orElseThrow(() -> com.skillproof.exception.ApiException.notFound(
                        "Skill '" + name + "' is not in your profile"));
    }
}
