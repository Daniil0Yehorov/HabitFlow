package com.habitFlow.habitService.config;

import com.habitFlow.habitService.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ServiceTokenProvider tokenProvider;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public Long getUserIdByUsername(String username) {
        String token = tokenProvider.getServiceToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        String url = userServiceUrl + "/auth/internal/username/" + username;

        try {
            ResponseEntity<UserDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    UserDto.class
            );

            if (response.getBody() == null) {
                throw new RuntimeException("User not found: " + username);
            }

            return response.getBody().getId();

        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException("Error fetching userId: " + ex.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Internal error fetching userId");
        }
    }
}