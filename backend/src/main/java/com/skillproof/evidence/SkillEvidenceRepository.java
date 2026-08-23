package com.skillproof.evidence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillEvidenceRepository extends JpaRepository<SkillEvidence, Long> {
    List<SkillEvidence> findByUserSkillIdOrderByOccurredAtDesc(Long userSkillId);
    List<SkillEvidence> findByUserIdOrderByOccurredAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);
}
