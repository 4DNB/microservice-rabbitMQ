package com.example.microservice.user.presentation.rest.dto;

import com.example.microservice.user.domain.model.UserRole;
import com.example.microservice.user.domain.model.UserStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

