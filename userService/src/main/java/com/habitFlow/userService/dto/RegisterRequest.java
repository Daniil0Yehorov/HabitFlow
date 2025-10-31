package com.habitFlow.userService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request for user registration")
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 64)
    @Schema(description = "Username for login", example = "darkwoodik12311")
    private String username;

    @NotBlank
    @Size(min = 6)
    @Schema(description = "Password for login", example = "darkwoodik1231")
    private String password;

    @NotBlank
    @Email
    @Schema(description = "User email address", example = "dakew23sf23fg@gmail.com")
    private String email;
}