package com.skillproof.interview;

import com.skillproof.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "skill_ids", columnDefinition = "text")
    private String skillIds;

    @Column(nullable = false)
    private String status = "IN_PROGRESS";

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String t) { this.targetRole = t; }
    public String getSkillIds() { return skillIds; }
    public void setSkillIds(String s) { this.skillIds = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer s) { this.overallScore = s; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant c) { this.completedAt = c; }
}
