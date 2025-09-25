package com.habitFlow.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitFlow.userService.dto.LoginRequest;
import com.habitFlow.userService.dto.RegisterRequest;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.repository.RefreshTokenRepository;
import com.habitFlow.userService.config.JwtUtil;
import com.habitFlow.userService.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.habitFlow.userService.repository.UserRepository;
import static org.hamcrest.Matchers.containsString;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private RegisterRequest registerRequest;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest("testuser", "password123");
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered!"));
    }

    @Test
    void shouldFailOnDuplicateRegistration() throws Exception {

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());


        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldFailOnWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("testuser", "wrongpass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());


        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void shouldReturn400ForInvalidRegisterRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "123", "not-an-email");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("Username is required"))
                .andExpect(jsonPath("$.password").value("Password must be at least 6 characters"))
                .andExpect(jsonPath("$.email").value("Invalid email format"));
    }

    @Test
    void shouldReturn400ForInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("nonexistent-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"));
    }

    @Test
    void shouldReturn409ForDuplicateUsernameOrEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "password123", "test@example.com");
        userService.registerUser(User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username or email already exists"));
    }

    @Test
    void shouldReturn401ForExpiredJwt() throws Exception {
        String expiredToken = jwtUtil.generateExpiredToken("testuser");

        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error",
                        containsString("JWT expired")));
    }

    @Test
    void shouldReturn401ForInvalidJwt() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error",
                        containsString("Invalid JWT")));

    }


}