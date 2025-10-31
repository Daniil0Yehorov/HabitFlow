package com.habitFlow.userService.controller;

import com.habitFlow.userService.dto.AuthResponse;
import com.habitFlow.userService.dto.LoginRequest;
import com.habitFlow.userService.dto.RegisterRequest;
import com.habitFlow.userService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register new user", description = "Registers a new user and initializes" +
            " notification settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "500", description = "Unexpected error"),
            @ApiResponse(responseCode = "502", description = "Notification Service unavailable")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Parameter(description = "Registration request containing user credentials and email",
                    required = true)
            @Valid @RequestBody RegisterRequest request) {
        String result = authService.register(request);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "User login", description = "Generates access and refresh tokens for a user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, tokens returned"),
            @ApiResponse(responseCode = "400", description = "Invalid login data"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or unverified email"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected error")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Parameter(description = "Login request containing username/email and password",
                    required = true)
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Refresh access token", description = "Generates new access token using a valid refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token refreshed"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token"),
            @ApiResponse(responseCode = "500", description = "Unexpected error")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Parameter(description = "Refresh token string used to generate new access token",
                    example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            @RequestBody String refreshTokenStr
    ) {
        AuthResponse response = authService.refresh(refreshTokenStr);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout user", description = "Revokes refresh token and logs out the user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "500", description = "Unexpected error")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @Parameter(description = "Refresh token to invalidate during logout", required = true)
            @RequestBody String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok("Logged out successfully");
    }

    @Operation(summary = "Verify email", description = "Confirms user email using verification token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired verification token"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected error"),
            @ApiResponse(responseCode = "502", description = "Notification Service unavailable"),
    })
    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(
            @Parameter(description = "User email to verify", example = "user@example.com")
            @RequestParam String email,
            @Parameter(description = "Verification token sent to the user's email", example = "abc123xyz")
            @RequestParam String token) {
        String result = authService.verifyEmail(email, token);
        return ResponseEntity.ok(result);
    }
}
