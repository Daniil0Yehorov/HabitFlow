package com.habitFlow.habitService.config;

import com.habitFlow.habitService.dto.EmailRequest;
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

    public void sendEmail(EmailRequest emailRequest) {
        String token = tokenProvider.getServiceToken();
        if (token == null || token.isBlank()) {
            throw new RuntimeException("[NotificationClient] Service token is null or empty!");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = "http://NOTIFICATION-SERVICE/notifications/email";

        HttpEntity<EmailRequest> requestEntity = new HttpEntity<>(emailRequest, headers);
        System.out.println("[NotificationClient] Sending email to: " + emailRequest.getTo());
        System.out.println("[NotificationClient] Using token: " + token);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("[NotificationClient] Failed to send email, status: " + response.getStatusCode());
            }

            System.out.println("[NotificationClient] Email sent: " + response.getBody());

        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException("[NotificationClient] Error sending email: " + ex.getStatusCode(), ex);
        } catch (Exception e) {
            throw new RuntimeException("[NotificationClient] Internal error sending email", e);
        }
    }
}