package com.habitFlow.userService.controller;

import com.habitFlow.userService.config.JwtUtil;
import com.habitFlow.userService.config.NotificationClient;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.repository.UserRepository;
import com.habitFlow.userService.service.RefreshTokenService;
import com.habitFlow.userService.service.UserService;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class InternalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserService userService;

    private String serviceToken;
    private String userToken;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        serviceToken = jwtUtil.generateServiceToken("habit-service");
        userToken = jwtUtil.generateAccessToken("testUser");
    }

    // ================= /auth/internal/username/{username} =================

    @Test
    @DisplayName("✅ 200 - Successfully fetch user by username with ROLE_SERVICE")
    void getUserByUsername_success() throws Exception {
        User user = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password(passwordEncoder.encode("12345678dadda"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        mockMvc.perform(get("/auth/internal/username/{username}", "john_doe")
                        .header("Authorization", "Bearer " + serviceToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("❌ 404 - User not found by username")
    void getUserByUsername_notFound() throws Exception {
        mockMvc.perform(get("/auth/internal/username/{username}", "not_exist")
                        .header("Authorization", "Bearer " + serviceToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @DisplayName("❌ 403 - Forbidden when token is not a service token")
    void getUserByUsername_forbidden() throws Exception {
        User user = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password(passwordEncoder.encode("12345678fad"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        mockMvc.perform(get("/auth/internal/username/{username}", "john_doe")
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

    // ================= /auth/internal/id/{id} =================

    @Test
    @DisplayName("✅ 200 - User exists (ROLE_SERVICE)")
    void checkUserExists_success() throws Exception {
        User user = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password(passwordEncoder.encode("12345678aka"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        mockMvc.perform(get("/auth/internal/id/{id}", user.getId())
                        .header("Authorization", "Bearer " + serviceToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ 404 - User not found by ID")
    void checkUserExists_notFound() throws Exception {
        mockMvc.perform(get("/auth/internal/id/{id}", 999)
                        .header("Authorization", "Bearer " + serviceToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @DisplayName("❌ 403 - Forbidden when token is not a service token")
    void checkUserExists_unauthorized() throws Exception {
        mockMvc.perform(get("/auth/internal/id/{id}", 1)
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

    // ================= /auth/internal/ids =================

    @Test
    @DisplayName("✅ 200 - Successfully fetch multiple users with ROLE_SERVICE")
    void getUsersByIds_success() throws Exception {
        User user1 = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password(passwordEncoder.encode("12345678"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        User user2 = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password(passwordEncoder.encode("87654321"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.saveAll(List.of(user1, user2));

        String jsonBody = "[" + user1.getId() + "," + user2.getId() + "]";

        mockMvc.perform(post("/auth/internal/ids")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("john_doe"))
                .andExpect(jsonPath("$[1].username").value("alice"));
    }

    @Test
    @DisplayName("❌ 403 - Forbidden when token is not a service token (POST /auth/internal/ids)")
    void getUsersByIds_forbidden() throws Exception {
        User user1 = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password(passwordEncoder.encode("12345678"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        User user2 = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password(passwordEncoder.encode("87654321"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.saveAll(List.of(user1, user2));

        String jsonBody = "[" + user1.getId() + "," + user2.getId() + "]";

        mockMvc.perform(post("/auth/internal/ids")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

}
