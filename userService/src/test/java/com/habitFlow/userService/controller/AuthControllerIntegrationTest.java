package com.habitFlow.userService.controller;import com.fasterxml.jackson.databind.ObjectMapper;

import com.habitFlow.userService.config.JwtUtil;
import com.habitFlow.userService.config.NotificationClient;
import com.habitFlow.userService.dto.LoginRequest;
import com.habitFlow.userService.dto.RegisterRequest;
import com.habitFlow.userService.exception.custom.ExternalServiceException;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.repository.UserRepository;
import com.habitFlow.userService.service.AuthService;
import com.habitFlow.userService.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private final String BASE_URL = "/auth";

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        doNothing().when(notificationClient).sendVerificationEmail(any(), any(), any());
        doNothing().when(notificationClient).createInitialNotificationSettings(any());
        doNothing().when(notificationClient).confirmEmailChannel(any());
    }

    // ================= /auth/register =================

    @Test
    @DisplayName("✅ /register - 200 OK")
    void register_success() throws Exception {
        RegisterRequest req = new RegisterRequest("user1", "password",
                "u1@example.com");
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered! Please check your email for" +
                        " the verification code."));
    }

    @Test
    @DisplayName("❌ /register - 409 Conflict (duplicate username/email)")
    void register_conflict() throws Exception {
        User existing = User.builder()
                .username("user1")
                .email("u1@example.com")
                .password(passwordEncoder.encode("pass"))
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(existing);

        RegisterRequest req = new RegisterRequest("user1", "password",
                "u1@example.com");
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    @DisplayName("❌ /register - 400 Bad Request (validation)")
    void register_badRequest() throws Exception {

        RegisterRequest req = new RegisterRequest("u", "p",
                null);//username  min: 3; password min: 6; email must be not null

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());
    }

    @Test
    @DisplayName("❌ /register - 502 Bad Gateway (Notification Service unavailable)")
    void register_notificationServiceUnavailable() throws Exception {
        RegisterRequest req = new RegisterRequest("brokenUser", "password",
                "broken@example.com");

        doThrow(new ExternalServiceException("[NotificationClient] Notification Service unavailable"))
                .when(notificationClient).createInitialNotificationSettings(any());

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[NotificationClient] Notification" +
                        " Service unavailable"));
    }

    // ================= /auth/login =================

    @Test
    @DisplayName("✅ /login - 200 OK")
    void login_success() throws Exception {
        User user = User.builder()
                .username("loginUser")
                .email("login@example.com")
                .password(passwordEncoder.encode("password1"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        LoginRequest req = new LoginRequest("loginUser", "password1");
        
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("❌ /login - 401 Unauthorized (wrong password)")
    void login_invalidPassword() throws Exception {
        User user = User.builder()
                .username("username2")
                .email("u2@example.com")
                .password(passwordEncoder.encode("correct"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        LoginRequest req = new LoginRequest("username2", "wrongggg");
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    @DisplayName("❌ /login - 401 Unauthorized (unverified email)")
    void login_unverified() throws Exception {
        User user = User.builder()
                .username("username3")
                .email("u3@example.com")
                .password(passwordEncoder.encode("password1"))
                .emailVerified(false)
                .verificationCode("tok123")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        LoginRequest req = new LoginRequest("username3", "password1");
        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please verify your email before" +
                        " logging in"));
    }

    @Test
    @DisplayName("❌ /login - 404 Not Found (user missing)")
    void login_notFound() throws Exception {
        LoginRequest req = new LoginRequest("nousername", "password");

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    // ================= /auth/refresh =================

    @Test
    @DisplayName("✅ /refresh - 200 OK")
    void refresh_success() throws Exception {
        User user = User.builder()
                .username("refreshUser")
                .email("refresh@example.com")
                .password(passwordEncoder.encode("password1"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);
        String token = refreshTokenService.createRefreshToken(user).getToken();

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").value(token));
    }

    @Test
    @DisplayName("❌ /refresh - 400  Invalid refresh token")
    void refresh_invalid_refresh_token() throws Exception {
        User user = User.builder()
                .username("refreshUser2")
                .email("refresh2@example.com")
                .password(passwordEncoder.encode("password2"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("wrong-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid refresh token"));
    }

    // ================= /auth/logout =================

    @Test
    @DisplayName("✅ /logout - 200 OK")
    void logout_success() throws Exception {
        mockMvc.perform(post(BASE_URL + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("any-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));
    }

    // ================= /auth/verify =================

    @Test
    @DisplayName("✅ /verify - 200 OK")
    void verify_success() throws Exception {
        User user = User.builder()
                .username("verifyUser")
                .email("verify@example.com")
                .password(passwordEncoder.encode("passordveri1"))
                .emailVerified(false)
                .verificationCode("tok123")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        mockMvc.perform(get(BASE_URL + "/verify")
                        .param("email", "verify@example.com")
                        .param("token", "tok123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Email verified successfully! You can now log in."));
    }

    @Test
    @DisplayName("❌ /verify - 400 Bad Request (invalid token)")
    void verify_invalidToken() throws Exception {
        User user = User.builder()
                .username("verifyUser2")
                .email("verify2@example.com")
                .password(passwordEncoder.encode("password6"))
                .emailVerified(false)
                .verificationCode("tok123")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        mockMvc.perform(get(BASE_URL + "/verify")
                        .param("email", "verify2@example.com")
                        .param("token", "wrong"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired verification" +
                        " token"));
    }

    @Test
    @DisplayName("❌ /verify - 404 Not Found (user missing)")
    void verify_userNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/verify")
                        .param("email", "nouser@example.com")
                        .param("token", "any"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @DisplayName("❌ /verify - 502 Bad Gateway (Notification Service unavailable)")
    void verify_notificationServiceUnavailable() throws Exception {
        User user = User.builder()
                .username("verifyUser3")
                .email("verify3@example.com")
                .password(passwordEncoder.encode("password6"))
                .emailVerified(false)
                .verificationCode("tok123")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        doThrow(new ExternalServiceException("[NotificationClient] Notification Service unavailable"))
                .when(notificationClient).confirmEmailChannel(any());

        mockMvc.perform(get(BASE_URL + "/verify")
                        .param("email", "verify3@example.com")
                        .param("token", "tok123"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[NotificationClient] Notification" +
                        " Service unavailable"));
    }
}