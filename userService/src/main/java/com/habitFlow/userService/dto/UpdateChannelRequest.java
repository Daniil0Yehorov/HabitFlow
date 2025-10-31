package com.habitFlow.userService.dto;

import com.habitFlow.userService.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to update notification channel for the current user")
public class UpdateChannelRequest {
    @NotNull(message = "Channel cannot be null. Allowed values: EMAIL, TG, NONE.")
    @Schema(description = "New notification channel", example = "TG")
    private NotificationChannel channel;
}