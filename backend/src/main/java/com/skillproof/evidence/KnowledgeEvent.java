package com.skillproof.evidence;

import com.skillproof.skill.UserSkill;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "knowledge_events")
public class KnowledgeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_skill_id")
    private UserSkill userSkill;

    @Column(name = "initial_retention", nullable = false)
    private double initialRetention;

    @Column(name = "review_score")
    private Integer reviewScore;

    @Column(name = "memory_strength", nullable = false)
    private double memoryStrength;

    @Column(name = "elapsed_days", nullable = false)
    private int elapsedDays;

    @Column(name = "predicted_retention", nullable = false)
    private double predictedRetention;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public UserSkill getUserSkill() { return userSkill; }
    public void setUserSkill(UserSkill us) { this.userSkill = us; }
    public double getInitialRetention() { return initialRetention; }
    public void setInitialRetention(double v) { this.initialRetention = v; }
    public Integer getReviewScore() { return reviewScore; }
    public void setReviewScore(Integer v) { this.reviewScore = v; }
    public double getMemoryStrength() { return memoryStrength; }
    public void setMemoryStrength(double v) { this.memoryStrength = v; }
    public int getElapsedDays() { return elapsedDays; }
    public void setElapsedDays(int v) { this.elapsedDays = v; }
    public double getPredictedRetention() { return predictedRetention; }
    public void setPredictedRetention(double v) { this.predictedRetention = v; }
    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant v) { this.reviewedAt = v; }
    public Instant getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(Instant v) { this.nextReviewAt = v; }
    public Instant getCreatedAt() { return createdAt; }
}
