package com.example.microservice.user.infrastructure.persistence;

import com.example.microservice.user.domain.model.User;
import com.example.microservice.user.domain.model.UserRole;
import com.example.microservice.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.example.microservice.user.infrastructure.persistence.repository.SpringDataJpaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({UserRepositoryAdapter.class, UserPersistenceMapper.class})
class UserRepositoryAdapterTest {

    @Autowired
    private UserRepositoryAdapter userRepository;

    @Autowired
    private SpringDataJpaUserRepository jpaRepository;

    @BeforeEach
    void setUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and retrieve a user by ID")
    void shouldSaveAndFindUserById() {
        User user = User.createNew("John Doe", "john@example.com", UserRole.CUSTOMER);

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("John Doe", saved.getName());
        assertEquals("john@example.com", saved.getEmail());

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindByEmail() {
        User user = User.createNew("Jane Doe", "jane@example.com", UserRole.SELLER);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("jane@example.com");
        assertTrue(found.isPresent());
        assertEquals("Jane Doe", found.get().getName());
    }

    @Test
    @DisplayName("Should check if user exists by email and id")
    void shouldCheckExistence() {
        User user = User.createNew("Alice", "alice@example.com", UserRole.ADMIN);
        User saved = userRepository.save(user);

        assertTrue(userRepository.existsByEmail("alice@example.com"));
        assertFalse(userRepository.existsByEmail("nonexisting@example.com"));

        assertTrue(userRepository.existsById(saved.getId()));
        assertFalse(userRepository.existsById(9999L));
    }

    @Test
    @DisplayName("Should retrieve all users")
    void shouldFindAllUsers() {
        userRepository.save(User.createNew("User 1", "u1@example.com", UserRole.CUSTOMER));
        userRepository.save(User.createNew("User 2", "u2@example.com", UserRole.MANAGER));

        List<User> all = userRepository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("Should delete user by ID")
    void shouldDeleteById() {
        User saved = userRepository.save(User.createNew("To Delete", "delete@example.com", UserRole.CUSTOMER));

        userRepository.deleteById(saved.getId());

        assertFalse(userRepository.existsById(saved.getId()));
    }
}

