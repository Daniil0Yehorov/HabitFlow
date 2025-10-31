package com.habitFlow.userService.config;

import com.habitFlow.userService.dto.EmailRequest;
import com.habitFlow.userService.dto.NotificationSettingsRequest;
import com.habitFlow.userService.dto.UpdateChannelClientRequest;
import com.habitFlow.userService.exception.custom.ExternalServiceException;
import com.habitFlow.userService.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final ServiceTokenProvider tokenProvider;

    private HttpHeaders buildHeaders() {
        String token = tokenProvider.getServiceToken();
        if (token == null || token.isBlank()) {
            throw new ExternalServiceException("[NotificationClient] Service token is null or empty!");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> void postRequest(String url, T body) {
        HttpEntity<T> entity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ExternalServiceException("[NotificationClient] Failed request to Notification Service," +
                        " status: "
                        + response.getStatusCode());
            }
            System.out.println("[NotificationClient] ✅ Request successful: " + url);
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException("[NotificationClient] Notification service returned: "
                    + ex.getStatusCode(), ex);
        } catch (Exception e) {
            throw new ExternalServiceException("[NotificationClient] Error communicating with Notification Service",
                    e);
        }
    }

    public void createInitialNotificationSettings(NotificationSettingsRequest request) {
        postRequest("http://NOTIFICATION-SERVICE/notifications/create-settings", request);
    }

    public void updateNotificationChannel(NotificationSettingsRequest request, NotificationChannel channel) {
        UpdateChannelClientRequest body = new UpdateChannelClientRequest();
        body.setUserId(request.getUserId());
        body.setChannel(channel);
        postRequest("http://NOTIFICATION-SERVICE/notifications/update-channel", body);
    }

    public void regenerateTelegramToken(NotificationSettingsRequest request) {
        postRequest("http://NOTIFICATION-SERVICE/notifications/regenerate-tg-token", request);
    }

    public void sendVerificationEmail(String to, String subject, String message) {
        EmailRequest body = new EmailRequest(to, subject, message);
        postRequest("http://NOTIFICATION-SERVICE/notifications/email", body);
    }

    public void confirmEmailChannel(NotificationSettingsRequest request) {
        postRequest("http://NOTIFICATION-SERVICE/notifications/confirm-email", request);
    }
}