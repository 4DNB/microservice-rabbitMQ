package com.example.microservice.user.presentation.rest.dto;

import com.example.microservice.user.domain.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "El nombre no puede estar vacío")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String name,

        @NotBlank(message = "El correo electrónico no puede estar vacío")
        @Email(message = "El formato del correo electrónico no es válido")
        String email,

        UserRole role
) {}

