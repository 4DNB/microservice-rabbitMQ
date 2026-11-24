package com.example.microservice.user.application.usecase;

import com.example.microservice.user.domain.model.User;

public interface GetUserByIdUseCase {
    User getUserById(Long id);
}

