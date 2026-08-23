package com.skillproof.skill;

import com.skillproof.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skills;

    public SkillController(SkillService skills) {
        this.skills = skills;
    }

    @GetMapping("/catalog")
    public List<SkillService.CatalogItem> catalog(@RequestParam(required = false) String q) {
        return skills.catalog(q);
    }

    @GetMapping("/at-risk")
    public List<SkillService.SkillRow> atRisk(@CurrentUserId Long userId) {
        return skills.atRisk(userId);
    }

    @GetMapping("/{id}")
    public SkillService.SkillDetail detail(@CurrentUserId Long userId, @PathVariable Long id) {
        return skills.detail(userId, id);
    }

    @DeleteMapping("/{id}")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void unclaim(@CurrentUserId Long userId, @PathVariable Long id) {
        skills.unclaim(userId, id);
    }
}
