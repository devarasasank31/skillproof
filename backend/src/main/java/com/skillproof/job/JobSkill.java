package com.skillproof.job;

import com.skillproof.skill.Skill;
import jakarta.persistence.*;

@Entity
@Table(name = "job_skills")
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private JobDescription job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "matched_name", nullable = false)
    private String matchedName;

    private int frequency = 1;

    private Integer confidence;

    public Long getId() { return id; }
    public JobDescription getJob() { return job; }
    public void setJob(JobDescription j) { this.job = j; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill s) { this.skill = s; }
    public String getMatchedName() { return matchedName; }
    public void setMatchedName(String m) { this.matchedName = m; }
    public int getFrequency() { return frequency; }
    public void setFrequency(int f) { this.frequency = f; }
    public Integer getConfidence() { return confidence; }
    public void setConfidence(Integer c) { this.confidence = c; }
}
