package com.habitFlow.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request for user login")
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @Schema(description = "User's login name", example = "darkwoodik12311")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User's password", example = "darkwoodik1231")
    private String password;
}
