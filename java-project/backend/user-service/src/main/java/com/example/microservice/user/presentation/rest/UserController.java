package com.example.microservice.user.presentation.rest;

import com.example.microservice.user.application.dto.CreateUserCommand;
import com.example.microservice.user.application.dto.UpdateUserCommand;
import com.example.microservice.user.application.usecase.*;
import com.example.microservice.user.domain.model.User;
import com.example.microservice.user.presentation.rest.dto.CreateUserRequest;
import com.example.microservice.user.presentation.rest.dto.UpdateUserRequest;
import com.example.microservice.user.presentation.rest.dto.UserResponse;
import com.example.microservice.user.presentation.rest.mapper.UserRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Presentation Layer REST Controller.
 * Entry point for HTTP requests into the User Microservice.
 * Translates HTTP requests to Application Use Cases and responses to Presentation DTOs.
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final GetAllUsersUseCase getAllUsersUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserRestMapper mapper;

    public UserController(
            CreateUserUseCase createUserUseCase,
            GetUserByIdUseCase getUserByIdUseCase,
            GetAllUsersUseCase getAllUsersUseCase,
            UpdateUserUseCase updateUserUseCase,
            DeleteUserUseCase deleteUserUseCase,
            UserRestMapper mapper
    ) {
        this.createUserUseCase = createUserUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.getAllUsersUseCase = getAllUsersUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        CreateUserCommand command = mapper.toCommand(request);
        User createdUser = createUserUseCase.createUser(command);
        UserResponse response = mapper.toResponse(createdUser);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = getUserByIdUseCase.getUserById(id);
        return ResponseEntity.ok(mapper.toResponse(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = getAllUsersUseCase.getAllUsers();
        return ResponseEntity.ok(mapper.toResponseList(users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UpdateUserCommand command = mapper.toCommand(id, request);
        User updatedUser = updateUserUseCase.updateUser(command);
        return ResponseEntity.ok(mapper.toResponse(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        deleteUserUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

