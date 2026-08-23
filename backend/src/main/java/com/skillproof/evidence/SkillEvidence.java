package com.skillproof.evidence;

import com.skillproof.skill.UserSkill;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "skill_evidence")
public class SkillEvidence {

    public enum Type { CLAIM, KNOWLEDGE, PRACTICAL, ACTIVITY, MARKET, GITHUB, RESUME }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_skill_id")
    private UserSkill userSkill;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false)
    private Type evidenceType;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private int points;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public UserSkill getUserSkill() { return userSkill; }
    public void setUserSkill(UserSkill us) { this.userSkill = us; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Type getEvidenceType() { return evidenceType; }
    public void setEvidenceType(Type t) { this.evidenceType = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public int getPoints() { return points; }
    public void setPoints(int p) { this.points = p; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant o) { this.occurredAt = o; }
    public Instant getCreatedAt() { return createdAt; }
}
