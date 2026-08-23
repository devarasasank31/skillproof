package com.skillproof.challenge;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "practical_challenges")
public class PracticalChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String difficulty = "MEDIUM";

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(columnDefinition = "text")
    private String rubric;

    @Column(name = "required_keywords", columnDefinition = "text")
    private String requiredKeywords;

    @Column(name = "est_minutes", nullable = false)
    private int estMinutes = 30;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public void setSlug(String s) { this.slug = s; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String s) { this.skillName = s; }
    public String getType() { return type; }
    public void setType(String t) {
        if (!java.util.List.of("CODING", "DEBUGGING", "SQL", "REST_API", "SYSTEM_DESIGN", "ARCHITECTURE", "CONCEPT", "INTERVIEW").contains(t)) {
            throw new IllegalArgumentException("Invalid challenge type: " + t);
        }
        this.type = t;
    }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String d) { this.difficulty = d; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String p) { this.prompt = p; }
    public String getRubric() { return rubric; }
    public void setRubric(String r) { this.rubric = r; }
    public String getRequiredKeywords() { return requiredKeywords; }
    public void setRequiredKeywords(String k) { this.requiredKeywords = k; }
    public int getEstMinutes() { return estMinutes; }
    public void setEstMinutes(int e) { this.estMinutes = e; }
    public Instant getCreatedAt() { return createdAt; }
}
