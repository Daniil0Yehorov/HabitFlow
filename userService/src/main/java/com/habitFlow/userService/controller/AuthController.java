package com.habitFlow.userService.controller;


import com.habitFlow.userService.dto.AuthResponse;
import com.habitFlow.userService.dto.LoginRequest;
import com.habitFlow.userService.dto.RegisterRequest;
import com.habitFlow.userService.model.RefreshToken;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.service.JwtUtil;
import com.habitFlow.userService.service.RefreshTokenService;
import com.habitFlow.userService.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        userService.registerUser(user);
        return ResponseEntity.ok("User registered!");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User found = userService.findByUsername(request.getUsername());

        if (found != null && passwordEncoder.matches(request.getPassword(),
                found.getPassword())) {

            String accessToken = jwtUtil.generateAccessToken(found.getUsername());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(found);

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
        }
        return ResponseEntity.status(401).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody String refreshTokenStr) {
        RefreshToken rt = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!refreshTokenService.validateRefreshToken(rt)) {
            return ResponseEntity.status(401).build();
        }

        String newAccessToken = jwtUtil.generateAccessToken(rt.getUser().getUsername());
        return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshTokenStr));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
        return ResponseEntity.ok("Logged out successfully");
    }
}
