package com.habitFlow.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitFlow.userService.config.JwtUtil;
import com.habitFlow.userService.config.NotificationClient;
import com.habitFlow.userService.dto.UpdateChannelRequest;
import com.habitFlow.userService.exception.custom.ChannelNotSelectedException;
import com.habitFlow.userService.exception.custom.ExternalServiceException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import com.habitFlow.userService.model.NotificationChannel;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserService userService;

    @MockBean
    private NotificationClient notificationClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    private User testUser;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();

        testUser = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password(passwordEncoder.encode("1234password"))
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(testUser);

        userToken = jwtUtil.generateAccessToken("john_doe");

        doNothing().when(notificationClient).regenerateTelegramToken(any());
        doNothing().when(notificationClient).updateNotificationChannel(any(), eq(NotificationChannel.TG));
    }

    // ================= /user/me =================

    @Test
    @DisplayName("✅ 200 - Successfully fetch current user info")
    void getCurrentUserInfo_success() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("❌ 401 - Unauthorized (missing token)")
    void getCurrentUserInfo_unauthorized() throws Exception {
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("JWT error: Full authentication is" +
                        " required to access this resource"));
    }

    @Test
    @DisplayName("❌ 404 - User not found")
    void getCurrentUserInfo_userNotFound() throws Exception {
        userRepository.deleteAll();
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    // ================= /user/notification-channel =================

    @Test
    @DisplayName("✅ 200 - Successfully update notification channel")
    void updateNotificationChannel_success() throws Exception {

        UpdateChannelRequest request= new UpdateChannelRequest();
        request.setChannel(NotificationChannel.TG);

        mockMvc.perform(post("/user/notification-channel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification channel updated to TG"));
    }

    @Test
    @DisplayName("❌ 400 - Invalid request (missing channel field)")
    void updateNotificationChannel_invalidRequest() throws Exception {
        UpdateChannelRequest request = new UpdateChannelRequest();

        mockMvc.perform(post("/user/notification-channel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.channel")
                        .value("Channel cannot be null. Allowed values: EMAIL, TG, NONE."));
    }

    @Test
    @DisplayName("❌ 401 - Unauthorized (missing token)")
    void updateNotificationChannel_unauthorized() throws Exception {
        UpdateChannelRequest request= new UpdateChannelRequest();
        request.setChannel(NotificationChannel.EMAIL);

        mockMvc.perform(post("/user/notification-channel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("JWT error: Full authentication is" +
                        " required to access this resource"));
    }

    @Test
    @DisplayName("❌ 403 - Telegram channel not selected (update channel)")
    void updateNotificationChannel_forbidden() throws Exception {
        doThrow(new ChannelNotSelectedException("Telegram channel is not selected"))
                .when(notificationClient)
                .updateNotificationChannel(any(), eq(NotificationChannel.TG));
        UpdateChannelRequest request= new UpdateChannelRequest();
        request.setChannel(NotificationChannel.TG);

        mockMvc.perform(post("/user/notification-channel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Telegram channel is not selected"));
    }

    @Test
    @DisplayName("❌ 404 - User not found when updating channel")
    void updateNotificationChannel_userNotFound() throws Exception {
        userRepository.deleteAll();

        UpdateChannelRequest request= new UpdateChannelRequest();
        request.setChannel(NotificationChannel.TG);

        mockMvc.perform(post("/user/notification-channel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @DisplayName("❌ 500 - Notification service error (update channel)")
    void updateNotificationChannel_notificationServiceError() throws Exception {
        doThrow(new RestClientException("Service unavailable"))
                .when(notificationClient)
                .updateNotificationChannel(any(), eq(NotificationChannel.EMAIL));

        UpdateChannelRequest request= new UpdateChannelRequest();
        request.setChannel(NotificationChannel.EMAIL);

        mockMvc.perform(post("/user/notification-channel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Unexpected error: Notification service error: Service unavailable"));
    }

    @Test
    @DisplayName("❌ 502 - External service unavailable (update channel)")
    void updateNotificationChannel_externalServiceError() throws Exception {
        doThrow(new ExternalServiceException("Notification service unreachable"))
                .when(notificationClient)
                .updateNotificationChannel(any(), eq(NotificationChannel.EMAIL));

        UpdateChannelRequest request= new UpdateChannelRequest();
        request.setChannel(NotificationChannel.EMAIL);

        mockMvc.perform(post("/user/notification-channel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error")
                        .value("Notification service unreachable"));
    }

    // ================= /user/regenerate-tg-token =================

    @Test
    @DisplayName("✅ 200 - Telegram token regenerated successfully")
    void regenerateTelegramToken_success() throws Exception {

        mockMvc.perform(post("/user/regenerate-tg-token")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(content().string("A new Telegram token has been sent to your email."));
    }

    @Test
    @DisplayName("❌ 401 - Unauthorized (missing token)")
    void regenerateTelegramToken_unauthorized() throws Exception {
        mockMvc.perform(post("/user/regenerate-tg-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("JWT error: Full authentication is" +
                        " required to access this resource"));
    }

    @Test
    @DisplayName("❌ 403 - Telegram channel not selected (regenerate token)")
    void regenerateTelegramToken_forbidden() throws Exception {
        doThrow(new ChannelNotSelectedException("Telegram channel is not selected"))
                .when(notificationClient).regenerateTelegramToken(any());

        mockMvc.perform(post("/user/regenerate-tg-token")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Telegram channel is not selected"));
    }

    @Test
    @DisplayName("❌ 500 - Notification service error")
    void regenerateTelegramToken_notificationServiceError() throws Exception {
        doThrow(new RestClientException("Service unavailable"))
                .when(notificationClient).regenerateTelegramToken(any());

        mockMvc.perform(post("/user/regenerate-tg-token")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Unexpected error: Notification service error: Service unavailable"));
    }

    @Test
    @DisplayName("❌ 404 - User not found (regenerate token)")
    void regenerateTelegramToken_userNotFound() throws Exception {
        userRepository.deleteAll();
        mockMvc.perform(post("/user/regenerate-tg-token")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }

    @Test
    @DisplayName("❌ 502 - External service unavailable (regenerate token)")
    void regenerateTelegramToken_externalServiceError() throws Exception {
        doThrow(new ExternalServiceException("Notification service unreachable"))
                .when(notificationClient)
                .regenerateTelegramToken(any());

        mockMvc.perform(post("/user/regenerate-tg-token")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error")
                        .value("Notification service unreachable"));
    }
}