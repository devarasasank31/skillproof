package com.skillproof.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
    List<JobDescription> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<JobDescription> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
}
