package com.skillproof.github;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitHubRepoRepository extends JpaRepository<GitHubRepository, Long> {
    Optional<GitHubRepository> findByProfileIdAndExternalId(Long profileId, Long externalId);
}
