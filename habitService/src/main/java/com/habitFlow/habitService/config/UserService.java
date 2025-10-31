package com.habitFlow.habitService.config;

import com.habitFlow.habitService.dto.UserDto;
import com.habitFlow.habitService.exception.custom.ExternalServiceException;
import com.habitFlow.habitService.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
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
            ResponseEntity<UserDto> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, UserDto.class);
            if (response.getBody() == null) throw new ResourceNotFoundException("User not found: " + username);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException("[UserService] Error: " + ex.getStatusCode(), ex);
        } catch (Exception e) {
            throw new ExternalServiceException("[UserService] Internal error", e);
        }
    }

    public boolean existsById(Long userId) {
        String token = tokenProvider.getServiceToken();
        if (token == null || token.isBlank()) {
            throw new ExternalServiceException("[UserService] Service token is null or empty!");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        String url = "http://USER-SERVICE/auth/internal/id/" + userId;

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new ExternalServiceException("[UserService] Error checking user existence: " + ex.getStatusCode(), ex);
        } catch (Exception e) {
            throw new ExternalServiceException("[UserService] Internal error checking user existence", e);
        }
    }

}