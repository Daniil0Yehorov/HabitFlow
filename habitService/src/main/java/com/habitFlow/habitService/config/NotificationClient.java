package com.habitFlow.habitService.config;

import com.habitFlow.habitService.dto.DispatchNotificationRequest;
import com.habitFlow.habitService.exception.custom.ExternalServiceException;
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
            throw new ExternalServiceException("[NotificationClient] ❌ Service token is null or empty!");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> void postRequest(String url, T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, buildHeaders());
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ExternalServiceException("[NotificationClient] ⚠️ Failed request to Notification Service,"
                        + " status: " + response.getStatusCode());
            }
            System.out.println("[NotificationClient] ✅ Request successful: " + url);
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException("[NotificationClient] ❌ Notification service returned: "
                    + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (Exception e) {
            throw new ExternalServiceException("[NotificationClient] 💥 Notification Service unavailable", e);
        }
    }

    public void dispatchNotification(String username, String subject, String message) {
        String url = "http://NOTIFICATION-SERVICE/notifications/dispatch";
        DispatchNotificationRequest requestBody = new DispatchNotificationRequest(username, subject, message);
        System.out.printf("[NotificationClient] 📤 Dispatching notification to '%s' with subject '%s'%n",
                username, subject);
        postRequest(url, requestBody);
    }
}