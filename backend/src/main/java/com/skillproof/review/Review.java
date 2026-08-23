package com.skillproof.review;

import com.skillproof.skill.UserSkill;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews", indexes = @Index(name = "idx_reviews_due", columnList = "due_at, status"))
public class Review {

    public enum Status { DUE, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_skill_id")
    private UserSkill userSkill;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.DUE;

    private Integer score;

    @Column(name = "interval_days")
    private Integer intervalDays;

    @Column(name = "memory_strength")
    private Double memoryStrength;

    @Column(name = "retention_before")
    private Double retentionBefore;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public UserSkill getUserSkill() { return userSkill; }
    public void setUserSkill(UserSkill us) { this.userSkill = us; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
    public Double getMemoryStrength() { return memoryStrength; }
    public void setMemoryStrength(Double memoryStrength) { this.memoryStrength = memoryStrength; }
    public Double getRetentionBefore() { return retentionBefore; }
    public void setRetentionBefore(Double retentionBefore) { this.retentionBefore = retentionBefore; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
