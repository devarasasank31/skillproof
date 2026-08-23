package com.skillproof.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedQuestionRepository extends JpaRepository<GeneratedQuestionEntity, Long> {

    List<GeneratedQuestionEntity> findBySkillNameIgnoreCase(String skillName);

    boolean existsBySkillNameIgnoreCaseAndPromptHash(String skillName, String promptHash);

    long countBySkillNameIgnoreCase(String skillName);
}
