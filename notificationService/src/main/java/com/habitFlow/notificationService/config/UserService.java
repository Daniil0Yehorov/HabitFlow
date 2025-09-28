package com.habitFlow.notificationService.config;

import com.habitFlow.notificationService.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserService {

    private final RestTemplate restTemplate;
    private final ServiceTokenProvider tokenProvider;

    public UserDto getUserByUsername(String username) {
        String token = tokenProvider.getServiceToken();
        if (token == null || token.isBlank()) {
            throw new RuntimeException("[UserService] Service token is null or empty!");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        String url = "http://USER-SERVICE/auth/internal/username/" + username;

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<UserDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    UserDto.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("[UserService] User not found: " + username);
            }

            return response.getBody();

        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException("[UserService] Error fetching userId: " + ex.getStatusCode(), ex);
        } catch (Exception e) {
            throw new RuntimeException("[UserService] Internal error fetching userId", e);
        }
    }
}