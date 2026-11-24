package com.example.microservice.user.application.service;

import com.example.microservice.user.application.dto.CreateUserCommand;
import com.example.microservice.user.application.dto.UpdateUserCommand;
import com.example.microservice.user.domain.exception.UserAlreadyExistsException;
import com.example.microservice.user.domain.exception.UserNotFoundException;
import com.example.microservice.user.domain.model.User;
import com.example.microservice.user.domain.model.UserRole;
import com.example.microservice.user.domain.model.UserStatus;
import com.example.microservice.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("Should successfully create user when email does not exist")
    void shouldCreateUserSuccessfully() {
        CreateUserCommand command = new CreateUserCommand("Alice", "alice@example.com", UserRole.CUSTOMER);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUser(command);

        assertNotNull(result);
        assertEquals("Alice", result.getName());
        assertEquals("alice@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when creating user with existing email")
    void shouldThrowExceptionWhenEmailAlreadyExistsOnCreate() {
        CreateUserCommand command = new CreateUserCommand("Alice", "alice@example.com", UserRole.CUSTOMER);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(command));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find user by id when present")
    void shouldFindUserById() {
        User user = User.createNew("Bob", "bob@example.com", UserRole.CUSTOMER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertEquals("Bob", result.getName());
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user id not found")
    void shouldThrowNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    @DisplayName("Should return all users")
    void shouldReturnAllUsers() {
        List<User> users = List.of(
                User.createNew("Bob", "bob@example.com", UserRole.CUSTOMER),
                User.createNew("Charlie", "charlie@example.com", UserRole.SELLER));
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUser() {
        User existingUser = User.reconstitute(1L, "Bob", "bob@example.com", UserRole.CUSTOMER, UserStatus.ACTIVE, null,
                null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("bob.new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserCommand command = new UpdateUserCommand(1L, "Bob New", "bob.new@example.com", UserRole.MANAGER,
                UserStatus.INACTIVE);
        User updated = userService.updateUser(command);

        assertEquals("Bob New", updated.getName());
        assertEquals("bob.new@example.com", updated.getEmail());
        assertEquals(UserRole.MANAGER, updated.getRole());
        assertEquals(UserStatus.INACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when updating with already registered email")
    void shouldThrowExceptionWhenUpdateEmailTaken() {
        User existingUser = User.reconstitute(1L, "Bob", "bob@example.com", UserRole.CUSTOMER, UserStatus.ACTIVE, null,
                null);
        User anotherUser = User.reconstitute(2L, "Alice", "alice@example.com", UserRole.ADMIN, UserStatus.ACTIVE, null,
                null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(anotherUser));

        UpdateUserCommand command = new UpdateUserCommand(1L, "Bob", "alice@example.com", UserRole.CUSTOMER,
                UserStatus.ACTIVE);

        assertThrows(UserAlreadyExistsException.class, () -> userService.updateUser(command));
    }

    @Test
    @DisplayName("Should delete user when exists")
    void shouldDeleteUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existing user")
    void shouldThrowWhenDeletingNonExisting() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).deleteById(anyLong());
    }
}
