package com.habitFlow.notificationService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for sending email notifications")
public class EmailRequest {
    @Schema(description = "Recipient email address", example = "user@example.com")
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String to;

    @Schema(description = "Email subject line", example = "Welcome to HabitFlow!")
    @NotBlank(message = "Subject is required")
    private String subject;

    @Schema(description = "Main body of the email", example = "Your account has been created successfully.")
    @NotBlank(message = "Message body is required")
    private String message;
}