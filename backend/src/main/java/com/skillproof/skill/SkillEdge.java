package com.skillproof.skill;

import jakarta.persistence.*;

@Entity
@Table(name = "skill_edges")
public class SkillEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_skill_id", nullable = false)
    private Long fromSkillId;

    @Column(name = "to_skill_id", nullable = false)
    private Long toSkillId;

    @Column(nullable = false)
    private String type;

    public Long getId() { return id; }
    public Long getFromSkillId() { return fromSkillId; }
    public void setFromSkillId(Long v) { this.fromSkillId = v; }
    public Long getToSkillId() { return toSkillId; }
    public void setToSkillId(Long v) { this.toSkillId = v; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
}
