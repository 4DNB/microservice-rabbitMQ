package com.example.microservice.user.domain.exception;

public class UserAlreadyExistsException extends DomainException {
    public UserAlreadyExistsException(String email) {
        super("Ya existe un usuario con el correo electrónico '" + email + "'");
    }
}
