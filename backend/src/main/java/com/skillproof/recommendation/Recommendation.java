package com.skillproof.recommendation;

import com.skillproof.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(nullable = false)
    private int priority = 50;

    @Column(name = "effort_minutes")
    private Integer effortMinutes;

    @Column(name = "skill_id")
    private Long skillId;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(name = "generated_on", nullable = false)
    private LocalDate generatedOn = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public String getActionType() { return actionType; }
    public void setActionType(String a) { this.actionType = a; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getReason() { return reason; }
    public void setReason(String r) { this.reason = r; }
    public int getPriority() { return priority; }
    public void setPriority(int p) {
        this.priority = Math.max(0, Math.min(100, p));
    }
    public Integer getEffortMinutes() { return effortMinutes; }
    public void setEffortMinutes(Integer e) { this.effortMinutes = e; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long s) { this.skillId = s; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public LocalDate getGeneratedOn() { return generatedOn; }
    public Instant getCreatedAt() { return createdAt; }
}
