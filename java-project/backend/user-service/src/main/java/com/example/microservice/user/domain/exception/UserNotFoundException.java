package com.example.microservice.user.domain.exception;

public class UserNotFoundException extends DomainException {
    public UserNotFoundException(Long id) {
        super("Usuario no encontrado con el identificador: " + id);
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
