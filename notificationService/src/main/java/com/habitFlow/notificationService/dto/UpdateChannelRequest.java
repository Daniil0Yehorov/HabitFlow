package com.habitFlow.notificationService.dto;

import com.habitFlow.notificationService.model.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "DTO for updating the preferred notification channel")
@AllArgsConstructor
@NoArgsConstructor
public class UpdateChannelRequest {
    @NotNull(message = "User ID: must not be null")
    @Schema(description = "User ID", example = "42")
    private Long userId;

    @NotNull(message = "channel: must not be null")
    @Schema(description = "Notification channel (EMAIL, TG, NONE.)", example = "TG")
    private NotificationChannel channel;
}