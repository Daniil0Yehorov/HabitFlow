package com.habitFlow.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification settings for a user")
public class NotificationSettingsRequest {
    @Schema(description = "ID of the user", example = "123")
    private Long userId;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email of the user", example = "john@example.com")
    private String email;
}
