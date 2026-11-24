package com.example.microservice.user.domain.exception;

public class InvalidUserDataException extends DomainException {
    public InvalidUserDataException(String message) {
        super(message);
    }
}
