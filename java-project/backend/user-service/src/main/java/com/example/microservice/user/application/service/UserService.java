package com.example.microservice.user.application.service;

import com.example.microservice.user.application.dto.CreateUserCommand;
import com.example.microservice.user.application.dto.UpdateUserCommand;
import com.example.microservice.user.application.usecase.*;
import com.example.microservice.user.domain.exception.UserAlreadyExistsException;
import com.example.microservice.user.domain.exception.UserNotFoundException;
import com.example.microservice.user.domain.model.User;
import com.example.microservice.user.domain.model.UserStatus;
import com.example.microservice.user.domain.repository.UserRepository;

import java.util.List;

/**
 * Clean Architecture Application Service (Use Case Interactor).
 * Implements use cases and orchestrates domain business logic.
 * Independent of web and database frameworks.
 */
public class UserService implements
        CreateUserUseCase,
        GetUserByIdUseCase,
        GetAllUsersUseCase,
        UpdateUserUseCase,
        DeleteUserUseCase {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(CreateUserCommand command) {
        String normalizedEmail = command.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException(normalizedEmail);
        }

        User newUser = User.createNew(command.name(), command.email(), command.role());
        return userRepository.save(newUser);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User updateUser(UpdateUserCommand command) {
        User user = userRepository.findById(command.id())
                .orElseThrow(() -> new UserNotFoundException(command.id()));

        String normalizedEmail = command.email().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new UserAlreadyExistsException(normalizedEmail);
                });

        user.updateProfile(command.name(), command.email(), command.role());

        if (command.status() != null) {
            if (command.status() == UserStatus.ACTIVE) {
                user.activate();
            } else if (command.status() == UserStatus.INACTIVE) {
                user.deactivate();
            } else if (command.status() == UserStatus.SUSPENDED) {
                user.suspend();
            }
        }

        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
}
