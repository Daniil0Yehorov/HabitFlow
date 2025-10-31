package com.habitFlow.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(description = "Request to send an email")
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequest {
    @Schema(description = "Recipient email address", example = "user@example.com")
    private String to;

    @Schema(description = "Email subject", example = "Welcome to HabitFlow!")
    private String subject;

    @Schema(description = "Email message body", example = "Hello, your account has been created successfully!")
    private String message;
}