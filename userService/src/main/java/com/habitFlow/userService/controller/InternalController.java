package com.habitFlow.userService.controller;

import com.habitFlow.userService.dto.UserDto;
import com.habitFlow.userService.service.InternalFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/internal")
@RequiredArgsConstructor
@Tag(name = "Internal services", description = "Internal endpoints for services")
@SecurityRequirement(name = "bearerAuth")
public class InternalController {

    private final InternalFacade internalFacade;

    @Operation(summary = "Get user by username (internal)", description = "Used by other services to" +
            " fetch user info by username")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found and returned successfully"),
            @ApiResponse(responseCode = "403", description = "Missing ROLE_SERVICE authority"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(
            @Parameter(description = "Username of the user", example = "john_doe")
            @PathVariable String username) {
        return internalFacade.getUserByUsername(username);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found and returned successfully"),
            @ApiResponse(responseCode = "403", description = "Missing ROLE_SERVICE authority"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/id/{id}")
    public ResponseEntity<Void> checkUserExists(
            @Parameter(description = "ID of the user", example = "123") @PathVariable Long id) {
        return internalFacade.checkUserExists(id);
    }
}
