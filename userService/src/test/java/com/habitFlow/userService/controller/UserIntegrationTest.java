package com.habitFlow.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitFlow.userService.dto.LoginRequest;
import com.habitFlow.userService.dto.RegisterRequest;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.repository.RefreshTokenRepository;
import com.habitFlow.userService.repository.UserRepository;
import com.habitFlow.userService.service.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;

    private String validToken;

    private String expiredToken;


    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();
        testUser = userRepository.save(testUser);

        validToken = jwtUtil.generateAccessToken(testUser.getUsername());

        expiredToken = jwtUtil.generateExpiredToken(testUser.getUsername());
    }

    @Test
    void shouldReturnUserInfoWithValidToken() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));
    }

    @Test
    void shouldReturn401ForExpiredToken() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401ForInvalidToken() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404IfUserNotFound() throws Exception {
        String tokenForNonexistentUser = jwtUtil.generateAccessToken("notexist");
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + tokenForNonexistentUser)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

}