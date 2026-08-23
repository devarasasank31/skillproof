package com.skillproof.assessment;

import com.skillproof.skill.Skill;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;

    @Column(name = "bank_key")
    private String bankKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(columnDefinition = "text")
    private String options;

    @Column(name = "answer_key", columnDefinition = "text")
    private String answerKey;

    @Column(columnDefinition = "text")
    private String keywords;

    @Column(columnDefinition = "text")
    private String explanation;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public Long getId() { return id; }
    public Assessment getAssessment() { return assessment; }
    public void setAssessment(Assessment a) { this.assessment = a; }
    public String getBankKey() { return bankKey; }
    public void setBankKey(String k) { this.bankKey = k; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill s) { this.skill = s; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String d) { this.difficulty = d; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String p) { this.prompt = p; }
    public String getOptions() { return options; }
    public void setOptions(String o) { this.options = o; }
    public String getAnswerKey() { return answerKey; }
    public void setAnswerKey(String a) { this.answerKey = a; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String k) { this.keywords = k; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String e) { this.explanation = e; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int i) { this.orderIndex = i; }
}
