package com.skillproof.evidence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeEventRepository extends JpaRepository<KnowledgeEvent, Long> {
    List<KnowledgeEvent> findByUserSkillIdOrderByCreatedAtDesc(Long userSkillId);
}
