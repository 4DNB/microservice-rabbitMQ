package com.example.microservice.user.application.usecase;

import com.example.microservice.user.domain.model.User;

import java.util.List;

public interface GetAllUsersUseCase {
    List<User> getAllUsers();
}

