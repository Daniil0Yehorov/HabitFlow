package com.habitFlow.userService.controller;

import com.habitFlow.userService.dto.UpdateChannelRequest;
import com.habitFlow.userService.dto.UserDto;
import com.habitFlow.userService.service.UserFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User management", description = "Endpoints for user profile and notification settings")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private  final UserFacade userFacade;

    @Operation(summary = "Get current user info", description = "Returns the logged-in user's" +
            " profile info")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User info retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - token missing or invalid"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe() {
        return userFacade.getCurrentUserInfo();
    }

    @Operation(
            summary = "Update current user's notification channel",
            description = "Updates the preferred notification channel for the logged-in user. " +
                    "Available values: EMAIL, TG, NONE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification channel updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing channel value"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Telegram channel is not selected"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error while updating channel"),
            @ApiResponse(responseCode = "502", description = "Notification service unavailable")
    })
    @PostMapping("/notification-channel")
    public ResponseEntity<String> updateNotificationChannel(
            @Parameter(description = "Request containing new preferred notification channel", required = true)
            @Valid @RequestBody UpdateChannelRequest req
    ) {
        return userFacade.updateNotificationChannel(req);
    }

    @Operation(summary = "Regenerate Telegram token", description = "Generates a new Telegram token" +
            " and sends it to user's email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Telegram token regenerated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — token missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Telegram channel is not selected"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error while regenerating token"),
            @ApiResponse(responseCode = "502", description = "Notification service unavailable")
    })
    @PostMapping("/regenerate-tg-token")
    public ResponseEntity<String> regenerateTelegramToken() {
        return userFacade.regenerateTelegramToken();
    }
}