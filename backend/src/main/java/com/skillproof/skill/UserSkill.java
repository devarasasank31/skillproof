package com.skillproof.skill;

import com.skillproof.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_skills",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_id"}))
public class UserSkill {

    public enum State { NEW, LEARNING, STRONG, STALE, AT_RISK, WEAK, MASTERED, OVERCLAIMED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "claim_source", nullable = false)
    private String claimSource = "MANUAL";

    @Column(name = "knowledge_score", nullable = false)
    private int knowledgeScore;

    @Column(name = "practical_score", nullable = false)
    private int practicalScore;

    @Column(name = "activity_score", nullable = false)
    private int activityScore;

    @Column(name = "market_score", nullable = false)
    private int marketScore;

    @Column(nullable = false)
    private int confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state = State.NEW;

    @Column(name = "memory_strength", nullable = false)
    private double memoryStrength = 20.0;

    @Column(nullable = false)
    private double retention = 1.0;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public Skill getSkill() {
        return skill;
    }
    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public String getClaimSource() {
        return claimSource;
    }
    public void setClaimSource(String claimSource) {
        this.claimSource = claimSource;
    }

    public int getKnowledgeScore() {
        return knowledgeScore;
    }
    public void setKnowledgeScore(int knowledgeScore) {
        this.knowledgeScore = clamp(knowledgeScore);
    }

    public int getPracticalScore() {
        return practicalScore;
    }
    public void setPracticalScore(int practicalScore) {
        this.practicalScore = clamp(practicalScore);
    }

    public int getActivityScore() {
        return activityScore;
    }
    public void setActivityScore(int activityScore) {
        this.activityScore = clamp(activityScore);
    }

    public int getMarketScore() {
        return marketScore;
    }
    public void setMarketScore(int marketScore) {
        this.marketScore = clamp(marketScore);
    }

    public int getConfidence() {
        return confidence;
    }
    public void setConfidence(int confidence) {
        this.confidence = Math.max(0, Math.min(100, confidence));
    }

    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }

    public double getMemoryStrength() {
        return memoryStrength;
    }
    public void setMemoryStrength(double memoryStrength) {
        if (memoryStrength < 1) memoryStrength = 1;
        if (memoryStrength > 365) memoryStrength = 365;
        this.memoryStrength = memoryStrength;
    }

    public double getRetention() {
        return retention;
    }
    public void setRetention(double retention) {
        if (retention < 0) retention = 0;
        if (retention > 1) retention = 1;
        this.retention = retention;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }
    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }
    public void setLastReviewedAt(Instant lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public Instant getNextReviewAt() {
        return nextReviewAt;
    }
    public void setNextReviewAt(Instant nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(100, v));
    }
}
