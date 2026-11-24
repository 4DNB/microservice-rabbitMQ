package com.example.microservice.user.application.dto;

import com.example.microservice.user.domain.model.UserRole;

public record CreateUserCommand(
                String name,
                String email,
                UserRole role) {
}
