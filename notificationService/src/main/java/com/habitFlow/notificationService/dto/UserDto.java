package com.habitFlow.notificationService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Minimal user DTO for notification operations")
public class UserDto {
    @Schema(description = "User ID", example = "42")
    private Long id;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email or external contact address", example = "user@example.com")
    private String email;
}
