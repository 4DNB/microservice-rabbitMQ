package com.example.microservice.user.infrastructure.persistence;

import com.example.microservice.user.domain.model.User;
import com.example.microservice.user.domain.repository.UserRepository;
import com.example.microservice.user.infrastructure.persistence.entity.UserJpaEntity;
import com.example.microservice.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.example.microservice.user.infrastructure.persistence.repository.SpringDataJpaUserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Clean Architecture Persistence Adapter / Gateway Implementation.
 * Implements the Domain UserRepository interface using Spring Data JPA.
 */
@Repository
@Transactional
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataJpaUserRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(SpringDataJpaUserRepository jpaRepository, UserPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}

