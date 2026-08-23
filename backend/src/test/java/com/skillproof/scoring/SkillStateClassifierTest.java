package com.skillproof.scoring;

import com.skillproof.skill.Skill;
import com.skillproof.skill.UserSkill;
import com.skillproof.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillStateClassifierTest {

    private final SkillStateClassifier classifier = new SkillStateClassifier();

    private UserSkill skill(int knowledge, int practical, int activity, int confidence) {
        User u = new User();
        u.setId(1L);
        Skill s = new Skill();
        s.setId(1L);
        s.setName("Kafka");
        s.setCategory("Data");
        UserSkill us = new UserSkill();
        us.setUser(u);
        us.setSkill(s);
        us.setKnowledgeScore(knowledge);
        us.setPracticalScore(practical);
        us.setActivityScore(activity);
        us.setConfidence(confidence);
        return us;
    }

    @Test
    void overclaimedWhenClaimedWithWeakKnowledgeAndNoEvidence() {
        UserSkill kafka = skill(29, 0, 0, 20);
        assertEquals(UserSkill.State.OVERCLAIMED, classifier.classify(kafka, 0.9, true));
    }

    @Test
    void notOverclaimedOncePracticalEvidenceExists() {
        UserSkill kafka = skill(29, 40, 0, 30);
        assertEquals(UserSkill.State.WEAK, classifier.classify(kafka, 0.9, true));
    }

    @Test
    void newWhenNoEvidenceAtAll() {
        UserSkill fresh = skill(0, 0, 0, 10);
        assertEquals(UserSkill.State.NEW, classifier.classify(fresh, 1.0, false));
    }

    @Test
    void staleWhenRetentionCollapses() {
        UserSkill us = skill(80, 60, 50, 72);
        assertEquals(UserSkill.State.STALE, classifier.classify(us, 0.45, true));
    }

    @Test
    void masteredRequiresAllHigh() {
        UserSkill us = skill(95, 85, 90, 88);
        assertEquals(UserSkill.State.MASTERED, classifier.classify(us, 0.95, true));
    }
}
