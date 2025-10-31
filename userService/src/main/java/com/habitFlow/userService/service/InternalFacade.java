package com.habitFlow.userService.service;

import com.habitFlow.userService.dto.UserDto;
import com.habitFlow.userService.exception.custom.ForbiddenException;
import com.habitFlow.userService.exception.custom.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * InternalFacade handles internal service-to-service user operations.
 * It centralizes checks and user retrieval logic for other services,
 * keeping InternalController clean and focused only on routing and Swagger documentation.
 */
@Service
@RequiredArgsConstructor
public class InternalFacade {

    private final UserService userService;

    public ResponseEntity<UserDto> getUserByUsername(String username) {
        if (!hasServiceRole()) {
            throw new ForbiddenException("Missing ROLE_SERVICE authority");
        }

        UserDto dto = userService.findUserDtoByUsername(username);
        if (dto == null)  throw new UserNotFoundException("User not found");

        return ResponseEntity.ok(dto);
    }

    public ResponseEntity<Void> checkUserExists(Long id) {
        if (!hasServiceRole()) {
            throw new ForbiddenException("Missing ROLE_SERVICE authority");
        }

        boolean exists = userService.existsById(id);
        if (!exists) throw new UserNotFoundException("User not found");

        return ResponseEntity.ok().build();
    }

    private boolean hasServiceRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SERVICE"));
    }
}