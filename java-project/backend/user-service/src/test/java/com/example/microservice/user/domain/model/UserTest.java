package com.example.microservice.user.domain.model;

import com.example.microservice.user.domain.exception.InvalidUserDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Should successfully create a new user with active status")
    void shouldCreateNewUser() {
        User user = User.createNew("John Doe", "john.doe@example.com", UserRole.CUSTOMER);

        assertNull(user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    @DisplayName("Should fail when name is empty or too short")
    void shouldFailWhenNameIsInvalid() {
        assertThrows(InvalidUserDataException.class, () ->
                User.createNew("", "john@example.com", UserRole.CUSTOMER));

        assertThrows(InvalidUserDataException.class, () ->
                User.createNew("A", "john@example.com", UserRole.CUSTOMER));
    }

    @Test
    @DisplayName("Should fail when email is invalid")
    void shouldFailWhenEmailIsInvalid() {
        assertThrows(InvalidUserDataException.class, () ->
                User.createNew("John Doe", "invalid-email", UserRole.CUSTOMER));

        assertThrows(InvalidUserDataException.class, () ->
                User.createNew("John Doe", "", UserRole.CUSTOMER));
    }

    @Test
    @DisplayName("Should update user profile")
    void shouldUpdateProfile() {
        User user = User.createNew("John Doe", "john@example.com", UserRole.CUSTOMER);

        user.updateProfile("Jane Doe", "jane@example.com", UserRole.ADMIN);

        assertEquals("Jane Doe", user.getName());
        assertEquals("jane@example.com", user.getEmail());
        assertEquals(UserRole.ADMIN, user.getRole());
    }

    @Test
    @DisplayName("Should change user status")
    void shouldChangeStatus() {
        User user = User.createNew("John Doe", "john@example.com", UserRole.CUSTOMER);

        user.deactivate();
        assertEquals(UserStatus.INACTIVE, user.getStatus());

        user.activate();
        assertEquals(UserStatus.ACTIVE, user.getStatus());

        user.suspend();
        assertEquals(UserStatus.SUSPENDED, user.getStatus());
    }
}

