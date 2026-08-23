package com.skillproof.github;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitHubProfileRepository extends JpaRepository<GitHubProfile, Long> {
    Optional<GitHubProfile> findByUserId(Long userId);
    long countByUserId(Long userId);
}
