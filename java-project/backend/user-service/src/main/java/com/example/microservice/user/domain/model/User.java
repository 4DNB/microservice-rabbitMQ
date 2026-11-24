package com.example.microservice.user.domain.model;

import com.example.microservice.user.domain.exception.InvalidUserDataException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

public class User {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final Long id;
    private String name;
    private String email;
    private UserRole role;
    private UserStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Private constructor to enforce creation via factories or builders
    private User(Long id, String name, String email, UserRole role, UserStatus status, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        validateName(name);
        validateEmail(email);
        Objects.requireNonNull(role, "User role cannot be null");
        Objects.requireNonNull(status, "User status cannot be null");

        this.id = id;
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.role = role;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    /**
     * Factory method for creating a new User (before persistence, id is null).
     */
    public static User createNew(String name, String email, UserRole role) {
        return new User(null, name, email, role != null ? role : UserRole.CUSTOMER, UserStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * Factory method for reconstituting an existing User from the persistence
     * layer.
     */
    public static User reconstitute(Long id, String name, String email, UserRole role, UserStatus status,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(id, name, email, role, status, createdAt, updatedAt);
    }

    public void updateProfile(String name, String email, UserRole role) {
        validateName(name);
        validateEmail(email);
        if (role != null) {
            this.role = role;
        }
        this.name = name.trim();
        this.email = email.trim().toLowerCase();
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidUserDataException("El nombre de usuario no puede estar vacío");
        }
        if (name.trim().length() < 2 || name.trim().length() > 100) {
            throw new InvalidUserDataException("El nombre de usuario debe tener entre 2 y 100 caracteres");
        }
    }

    private static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidUserDataException("El correo electrónico no puede estar vacío");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new InvalidUserDataException("Formato de correo electrónico no válido: " + email);
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
