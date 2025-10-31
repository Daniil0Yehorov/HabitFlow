package com.habitFlow.userService.dto;

import com.habitFlow.userService.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request to update notification channel for a user")
public class UpdateChannelClientRequest {
    @Schema(description = "ID of the user", example = "123")
    private Long userId;

    @Schema(description = "New notification channel", example = "EMAIL")
    private NotificationChannel channel;
}