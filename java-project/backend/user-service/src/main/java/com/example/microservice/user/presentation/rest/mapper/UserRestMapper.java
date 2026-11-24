package com.example.microservice.user.presentation.rest.mapper;

import com.example.microservice.user.application.dto.CreateUserCommand;
import com.example.microservice.user.application.dto.UpdateUserCommand;
import com.example.microservice.user.domain.model.User;
import com.example.microservice.user.presentation.rest.dto.CreateUserRequest;
import com.example.microservice.user.presentation.rest.dto.UpdateUserRequest;
import com.example.microservice.user.presentation.rest.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserRestMapper {

    public CreateUserCommand toCommand(CreateUserRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateUserCommand(
                request.name(),
                request.email(),
                request.role()
        );
    }

    public UpdateUserCommand toCommand(Long id, UpdateUserRequest request) {
        if (request == null) {
            return null;
        }
        return new UpdateUserCommand(
                id,
                request.name(),
                request.email(),
                request.role(),
                request.status()
        );
    }

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public List<UserResponse> toResponseList(List<User> users) {
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .map(this::toResponse)
                .toList();
    }
}

