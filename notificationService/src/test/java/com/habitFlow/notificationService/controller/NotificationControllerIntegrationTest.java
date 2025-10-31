package com.habitFlow.notificationService.controller;

import com.habitFlow.notificationService.config.JwtUtil;
import com.habitFlow.notificationService.config.MailConfig;
import com.habitFlow.notificationService.dto.DispatchNotificationRequest;
import com.habitFlow.notificationService.dto.EmailRequest;
import com.habitFlow.notificationService.dto.NotificationSettingsRequest;
import com.habitFlow.notificationService.dto.UpdateChannelRequest;
import com.habitFlow.notificationService.exception.custom.ForbiddenActionException;
import com.habitFlow.notificationService.exception.custom.NotificationNotFoundException;
import com.habitFlow.notificationService.exception.custom.NotificationSendException;
import com.habitFlow.notificationService.model.NotificationChannel;
import com.habitFlow.notificationService.repository.NotificationRepository;
import com.habitFlow.notificationService.service.NotificationFacade;
import com.habitFlow.notificationService.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailConfig mailConfig;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationFacade notificationFacade;

    private String serviceToken;
    private String userToken;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        notificationRepository.deleteAll();
        serviceToken = jwtUtil.generateServiceToken("habit-service");
        String userToken = jwtUtil.generateAccessToken("regular-user");
    }

    // ================= /notifications/email =================

    @Test
    @DisplayName("✅ 200 - Email sent successfully")
    void sendEmail_authorized() throws Exception {
        EmailRequest request = new EmailRequest("test@gmail.com","auth test","message ok");

        mockMvc.perform(post("/notifications/email")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ 400 - Invalid request (missing email)")
    void sendEmail_InvalidRequest() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setSubject("auth test");
        request.setMessage("message ok");

        mockMvc.perform(post("/notifications/email")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Recipient email is required"));
    }

    @Test
    @DisplayName("❌ 502 - Failed to send email")
    void sendEmail_FailedToSend() throws Exception {
        Mockito.when(notificationFacade.sendEmail(any(EmailRequest.class)))
                .thenThrow(new NotificationSendException("Failed to send email"));

        EmailRequest request = new EmailRequest("broken@gmail.com","Failure","SMTP failure");

        mockMvc.perform(post("/notifications/email")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Failed to send email"));
    }

    @Test
    @DisplayName("❌ 403 - Forbidden (Missing ROLE_SERVICE authority)")
    void sendEmail_ForbiddenService() throws Exception {

        EmailRequest request = new EmailRequest("test@gmail.com","Forbidden test","No acess");

        mockMvc.perform(post("/notifications/email")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

    @Test
    @DisplayName("❌ 404 - Notification settings not found")
    void sendEmail_NotificationNotFound() throws Exception {
        Mockito.when(notificationFacade.sendEmail(any(EmailRequest.class)))
                .thenThrow(new NotificationNotFoundException(
                        "Notification settings not found"));

        EmailRequest request = new EmailRequest("ghost@gmail.com","Missing","No settings");

        mockMvc.perform(post("/notifications/email")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notification settings not found"));
    }

    // ================= /notifications/create-settings =================

    @Test
    @DisplayName("✅ 200 - Settings created successfully")
    void createSettings_Success() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest(1L,"user@gmail.com",
                "userTest");

        mockMvc.perform(post("/notifications/create-settings")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ 400 - Invalid request (missing email)")
    void createSettings_InvalidRequest() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest();
        request.setUserId(1L);
        request.setUsername("usertest");

        mockMvc.perform(post("/notifications/create-settings")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Email is required"));
    }

    @Test
    @DisplayName("❌ 403 - Forbidden (missing ROLE_SERVICE authority) on create settings")
    void createSettings_Forbidden() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest(1L,"user@gmail.com",
                "userTest");

        mockMvc.perform(post("/notifications/create-settings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

    // ================= /notifications/update-channel =================

    @Test
    @DisplayName("✅ 200 - Channel updated successfully")
    void updateChannel_Success() throws Exception {
        UpdateChannelRequest request = new UpdateChannelRequest(1L, NotificationChannel.EMAIL);

        mockMvc.perform(post("/notifications/update-channel")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ 400 - Invalid request (missing channel)")
    void updateChannel_InvalidRequest() throws Exception {
        UpdateChannelRequest request = new UpdateChannelRequest();
        request.setUserId(1L);

        mockMvc.perform(post("/notifications/update-channel")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("channel: must not be null"));
    }

    @Test
    @DisplayName("❌ 403 - Forbidden (missing ROLE_SERVICE authority) on update-channel")
    void updateChannel_Forbidden_MissingRoleService() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest(1L,"user@gmail.com",
                "userTest");

        mockMvc.perform(post("/notifications/update-channel")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

    @Test
    @DisplayName("❌ 404 - Notification settings not found")
    void updateChannel_NotificationNotFound() throws Exception {
        Mockito.doThrow(new NotificationNotFoundException(
                        "Notification settings not found"))
                .when(notificationFacade)
                .updateChannel(any());

        UpdateChannelRequest request = new UpdateChannelRequest(999L, NotificationChannel.EMAIL);

        mockMvc.perform(post("/notifications/update-channel")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notification settings not found"));
    }

    // ================= /notifications/regenerate-tg-token =================

    @Test
    @DisplayName("✅ 200 - Telegram token regenerated successfully")
    void regenerateToken_Success() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest(1L,"user@gmail.com",
                "userTest");

        mockMvc.perform(post("/notifications/regenerate-tg-token")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ 400 - Invalid request (missing email)")
    void regenerateToken_InvalidRequest() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest();
        request.setUsername("userTest");
        request.setUserId(1L);

        mockMvc.perform(post("/notifications/regenerate-tg-token")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Email is required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("❌ 403 - Telegram channel not selected")
    void regenerateToken_Forbidden() throws Exception {
        Mockito.doThrow(new ForbiddenActionException(
                        "Telegram channel is not selected"))
                .when(notificationFacade).regenerateToken(any());

        NotificationSettingsRequest request = new NotificationSettingsRequest(1L,"user@gmail.com",
                "userTest");

        mockMvc.perform(post("/notifications/regenerate-tg-token")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Telegram channel is not selected"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("❌ 403 - Missing ROLE_SERVICE authority")
    void regenerateToken_ForbiddenMissingRoleService() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest(1L, "user@gmail.com",
                "userTest");

        mockMvc.perform(post("/notifications/regenerate-tg-token")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

    @Test
    @DisplayName("❌ 404 - Notification settings not found")
    void regenerateToken_NotFound() throws Exception {
        Mockito.doThrow(new NotificationNotFoundException(
                        "Notification settings not found"))
                .when(notificationFacade).regenerateToken(any());

        NotificationSettingsRequest request = new NotificationSettingsRequest(999L,"ghost@gmail.com",
                "ghostUser");

        mockMvc.perform(post("/notifications/regenerate-tg-token")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notification settings not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("❌ 502 - Failed to send email")
    void regenerateToken_BadGateway() throws Exception {
        Mockito.doThrow(new NotificationSendException(
                        "Failed to send email"))
                .when(notificationFacade).regenerateToken(any());

        NotificationSettingsRequest request = new NotificationSettingsRequest(1L,"user@gmail.com",
                "userTest");

        mockMvc.perform(post("/notifications/regenerate-tg-token")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Failed to send email"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ================= /notifications/dispatch =================

    @Test
    @DisplayName("✅ 200 - Notification dispatched successfully")
    void dispatchNotification_Success() throws Exception {
        DispatchNotificationRequest request = new DispatchNotificationRequest("userTest","Test subj" +
                "ect",
                "Test message");

        mockMvc.perform(post("/notifications/dispatch")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ 400 - Invalid request (missing message)")
    void dispatchNotification_InvalidRequest() throws Exception {
        DispatchNotificationRequest request = new DispatchNotificationRequest();
        request.setUsername("userTest");
        request.setSubject("Test subject");

        mockMvc.perform(post("/notifications/dispatch")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());}

    @Test
    @DisplayName("❌ 403 - Notification channel not confirmed")
    void dispatchNotification_Forbidden() throws Exception {
        Mockito.doThrow(new ForbiddenActionException(
                        "Notification channel not confirmed"))
                .when(notificationFacade).dispatchNotification(any());

        DispatchNotificationRequest request = new DispatchNotificationRequest();
        request.setUsername("userTest");
        request.setSubject("Test subject");
        request.setMessage("Test message");

        mockMvc.perform(post("/notifications/dispatch")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Notification channel not confirmed"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("❌ 403 - Missing ROLE_SERVICE authority")
    void dispatchNotification_Forbidden_MissingRole() throws Exception {
        DispatchNotificationRequest request = new DispatchNotificationRequest(
                "userTest", "Test subject", "Test message"
        );

        mockMvc.perform(post("/notifications/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

    @Test
    @DisplayName("❌ 404 - Notification settings not found")
    void dispatchNotification_NotFound() throws Exception {
        Mockito.doThrow(new NotificationNotFoundException("Notification settings not found"))
                .when(notificationFacade).dispatchNotification(any());

        DispatchNotificationRequest request = new DispatchNotificationRequest(
                "userTest", "Test subject", "Test message"
        );

        mockMvc.perform(post("/notifications/dispatch")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notification settings not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("❌ 502 - Failed to fetch user data from User Service")
    void dispatchNotification_BadGateway_UserServiceFailure() throws Exception {
        Mockito.doThrow(new NotificationSendException(
                        "Failed to fetch user data from User Service"))
                .when(notificationFacade).dispatchNotification(any());

        DispatchNotificationRequest request = new DispatchNotificationRequest();
        request.setUsername("userTest");
        request.setSubject("System failure");
        request.setMessage("Cannot fetch user data");

        mockMvc.perform(post("/notifications/dispatch")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Failed to fetch user data from User" +
                        " Service"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ================= /notifications/confirm-email =================

    @Test
    @DisplayName("✅ 200 - Email channel confirmed successfully")
    void confirmEmail_Success() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest(1L,"user@gmail.com",
                "userTest");

        mockMvc.perform(post("/notifications/confirm-email")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("❌ 400 - Invalid request (missing email)")
    void confirmEmail_InvalidRequest() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest();
        request.setUserId(1L);
        request.setUsername("userTest");

        mockMvc.perform(post("/notifications/confirm-email")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Email is required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("❌ 400 - Invalid request (missing userId)")
    void confirmEmail_InvalidRequest2() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest();
        request.setEmail("user@gmail.com");
        request.setUsername("userTest");

        mockMvc.perform(post("/notifications/confirm-email")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("User ID is required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("❌ 403 - Forbidden (Missing ROLE_SERVICE authority)")
    void confirmEmail_Forbidden_MissingRoleService() throws Exception {
        NotificationSettingsRequest request = new NotificationSettingsRequest(
                1L, "user@gmail.com", "userTest"
        );

        mockMvc.perform(post("/notifications/confirm-email")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Missing ROLE_SERVICE authority"));
    }

    @Test
    @DisplayName("❌ 404 - Notification settings not found")
    void confirmEmail_NotificationNotFound() throws Exception {
        Mockito.doThrow(new NotificationNotFoundException(
                        "Notification settings not found"))
                .when(notificationFacade).confirmEmail(any());

        NotificationSettingsRequest request = new NotificationSettingsRequest(999L,"ghost@gmail.com",
                "ghostUser");

        mockMvc.perform(post("/notifications/confirm-email")
                        .header("Authorization", "Bearer " + serviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Notification settings not found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
