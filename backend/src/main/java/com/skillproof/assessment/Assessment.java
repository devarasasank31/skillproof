package com.skillproof.assessment;

import com.skillproof.skill.Skill;
import com.skillproof.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assessments")
public class Assessment {

    public enum Status { IN_PROGRESS, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private String difficulty = "MEDIUM";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.IN_PROGRESS;

    @Column(nullable = false)
    private String source = "BANK";

    private Integer score;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User u) { this.user = u; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill s) { this.skill = s; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) {
        if (!"EASY".equals(difficulty) && !"MEDIUM".equals(difficulty) && !"HARD".equals(difficulty)) {
            throw new IllegalArgumentException("difficulty must be EASY, MEDIUM or HARD");
        }
        this.difficulty = difficulty;
    }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int q) { this.questionCount = q; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant c) { this.createdAt = c; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant c) { this.completedAt = c; }
}
