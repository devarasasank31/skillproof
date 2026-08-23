package com.skillproof.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCaseAndDeletedFalse(String email);
    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);
}
