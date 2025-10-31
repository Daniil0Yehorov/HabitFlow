package com.habitFlow.notificationService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "DTO for sending an internal message or notification")
@AllArgsConstructor
@NoArgsConstructor
public class DispatchNotificationRequest {
    @NotBlank(message = "username is required")
    @Schema(description = "Target username", example = "john_doe")
    private String username;

    @NotBlank(message = "subject is required")
    @Schema(description = "Optional message subject", example = "Reminder: Daily habit")
    private String subject;

    @NotBlank(message = "message is required")
    @Schema(description = "Message content to be sent", example = "Don't forget to log your habit today!")
    private String message;
}