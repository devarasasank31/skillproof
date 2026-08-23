package com.skillproof.skill;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "skill_scores")
public class SkillScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_skill_id")
    private UserSkill userSkill;

    @Column(nullable = false)
    private int confidence;

    @Column(nullable = false)
    private int knowledge;

    @Column(nullable = false)
    private int practical;

    @Column(nullable = false)
    private int activity;

    @Column(nullable = false)
    private int market;

    @Column(nullable = false)
    private String state;

    @Column(name = "snapshot_at", nullable = false, updatable = false)
    private Instant snapshotAt = Instant.now();

    public Long getId() { return id; }
    public UserSkill getUserSkill() { return userSkill; }
    public void setUserSkill(UserSkill us) { this.userSkill = us; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int c) { this.confidence = c; }
    public int getKnowledge() { return knowledge; }
    public void setKnowledge(int k) { this.knowledge = k; }
    public int getPractical() { return practical; }
    public void setPractical(int p) { this.practical = p; }
    public int getActivity() { return activity; }
    public void setActivity(int a) { this.activity = a; }
    public int getMarket() { return market; }
    public void setMarket(int m) { this.market = m; }
    public String getState() { return state; }
    public void setState(String s) { this.state = s; }
    public Instant getSnapshotAt() { return snapshotAt; }
}
