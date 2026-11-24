package com.example.microservice.user.application.usecase;

import com.example.microservice.user.application.dto.UpdateUserCommand;
import com.example.microservice.user.domain.model.User;

public interface UpdateUserUseCase {
    User updateUser(UpdateUserCommand command);
}

