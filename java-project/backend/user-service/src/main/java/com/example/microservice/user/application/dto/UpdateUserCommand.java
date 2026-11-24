package com.example.microservice.user.application.dto;

import com.example.microservice.user.domain.model.UserRole;
import com.example.microservice.user.domain.model.UserStatus;

public record UpdateUserCommand(
                Long id,
                String name,
                String email,
                UserRole role,
                UserStatus status) {
}
