package com.skillproof.skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByNameIgnoreCase(String name);
    List<Skill> findByCategory(String category);
    List<Skill> findByNameInIgnoreCase(Collection<String> names);
    org.springframework.data.domain.Page<Skill> findByNameContainingIgnoreCaseOrderByNameAsc(String name, org.springframework.data.domain.Pageable pageable);
}
