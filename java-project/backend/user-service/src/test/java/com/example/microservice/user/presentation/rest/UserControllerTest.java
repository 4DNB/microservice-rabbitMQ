package com.example.microservice.user.presentation.rest;

import com.example.microservice.user.domain.model.UserRole;
import com.example.microservice.user.domain.model.UserStatus;
import com.example.microservice.user.infrastructure.persistence.repository.SpringDataJpaUserRepository;
import com.example.microservice.user.presentation.rest.dto.CreateUserRequest;
import com.example.microservice.user.presentation.rest.dto.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SpringDataJpaUserRepository jpaRepository;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/users - Should create user and return 201 Created")
    void shouldCreateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Bruce Wayne", "bruce@wayne.com", UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Bruce Wayne"))
                .andExpect(jsonPath("$.email").value("bruce@wayne.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 400 Bad Request on validation error")
    void shouldReturn400WhenPayloadIsInvalid() throws Exception {
        CreateUserRequest request = new CreateUserRequest("", "not-an-email", null);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST /api/v1/users - Should return 409 Conflict when email already exists")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Clark Kent", "clark@dailyplanet.com", UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return user when exists")
    void shouldGetUserById() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Diana Prince", "diana@themyscira.com", UserRole.ADMIN);
        String responseBody = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(responseBody).get("id").asLong();

        mockMvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Diana Prince"))
                .andExpect(jsonPath("$.email").value("diana@themyscira.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - Should return 404 Not Found when user does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/users - Should return list of all users")
    void shouldGetAllUsers() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateUserRequest("User 1", "u1@test.com", UserRole.CUSTOMER))));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateUserRequest("User 2", "u2@test.com", UserRole.SELLER))));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Should update user")
    void shouldUpdateUser() throws Exception {
        CreateUserRequest createReq = new CreateUserRequest("Barry Allen", "barry@centralcity.com", UserRole.CUSTOMER);
        String createResponse = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        UpdateUserRequest updateReq = new UpdateUserRequest("Barry Allen Updated", "barry.flash@centralcity.com", UserRole.MANAGER, UserStatus.ACTIVE);

        mockMvc.perform(put("/api/v1/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Barry Allen Updated"))
                .andExpect(jsonPath("$.email").value("barry.flash@centralcity.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Should delete user and return 204 No Content")
    void shouldDeleteUser() throws Exception {
        CreateUserRequest createReq = new CreateUserRequest("Arthur Curry", "arthur@atlantis.com", UserRole.CUSTOMER);
        String createResponse = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/v1/users/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{id}", id))
                .andExpect(status().isNotFound());
    }
}

