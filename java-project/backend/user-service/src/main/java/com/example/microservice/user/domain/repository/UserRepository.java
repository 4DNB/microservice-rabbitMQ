package com.example.microservice.user.domain.repository;

import com.example.microservice.user.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Clean Architecture Domain Repository Gateway Interface.
 * Resides in the Domain layer as an abstraction of persistence requirements.
 */
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    boolean existsByEmail(String email);
    boolean existsById(Long id);
    void deleteById(Long id);
}

