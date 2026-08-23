package com.skillproof.interview;

import com.skillproof.skill.Skill;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(name = "answer_key", columnDefinition = "text")
    private String answerKey;

    @Column(columnDefinition = "text")
    private String keywords;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "answer_text", columnDefinition = "text")
    private String answerText;

    private Integer score;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "evaluation_source")
    private String evaluationSource;

    public Long getId() { return id; }
    public InterviewSession getSession() { return session; }
    public void setSession(InterviewSession s) { this.session = s; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill s) { this.skill = s; }
    public String getCategory() { return category; }
    public void setCategory(String c) { this.category = c; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String p) { this.prompt = p; }
    public String getAnswerKey() { return answerKey; }
    public void setAnswerKey(String a) { this.answerKey = a; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String k) { this.keywords = k; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int i) { this.orderIndex = i; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String t) { this.answerText = t; }
    public Integer getScore() { return score; }
    public void setScore(Integer s) { this.score = Math.max(0, Math.min(100, s)); }
    public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(Integer t) { this.timeSpentSeconds = t; }
    public String getEvaluationSource() { return evaluationSource; }
    public void setEvaluationSource(String e) { this.evaluationSource = e; }
}
