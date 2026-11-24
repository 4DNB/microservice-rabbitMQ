package com.example.microservice.user.infrastructure.config;

import com.example.microservice.user.application.service.UserService;
import com.example.microservice.user.application.usecase.*;
import com.example.microservice.user.domain.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clean Architecture Configuration / Dependency Injection Root.
 * Injects infrastructure implementations (UserRepositoryAdapter) into
 * application services (UserService) and provides UseCase interfaces to interface adapters.
 */
@Configuration
public class UserConfig {

    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(UserService userService) {
        return userService;
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserService userService) {
        return userService;
    }

    @Bean
    public GetAllUsersUseCase getAllUsersUseCase(UserService userService) {
        return userService;
    }

    @Bean
    public UpdateUserUseCase updateUserUseCase(UserService userService) {
        return userService;
    }

    @Bean
    public DeleteUserUseCase deleteUserUseCase(UserService userService) {
        return userService;
    }
}

