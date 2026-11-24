package com.example.microservice.user.application.usecase;

import com.example.microservice.user.application.dto.CreateUserCommand;
import com.example.microservice.user.domain.model.User;

public interface CreateUserUseCase {
    User createUser(CreateUserCommand command);
}

