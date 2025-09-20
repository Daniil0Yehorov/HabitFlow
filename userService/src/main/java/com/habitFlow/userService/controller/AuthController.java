package com.habitFlow.userService.controller;


import com.habitFlow.userService.model.RefreshToken;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.service.JwtUtil;
import com.habitFlow.userService.service.RefreshTokenService;
import com.habitFlow.userService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public String register(@RequestBody User user) {
        userService.registerUser(user);
        return "User registered!";
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User user) {
        User found = userService.findByUsername(user.getUsername());
        if (found != null && passwordEncoder.matches(user.getPassword(), found.getPassword())) {
            String accessToken = jwtUtil.generateAccessToken(found.getUsername(), 15 * 60 * 1000); // 15 мин
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(found);

            return Map.of(
                    "accessToken", accessToken,
                    "refreshToken", refreshToken.getToken()
            );
        }
        throw new RuntimeException("Invalid credentials");
    }

    @PostMapping("/refresh")
    public Map<String, String> refresh(@RequestBody Map<String, String> request) {
        String refreshTokenStr = request.get("refreshToken");

        RefreshToken rt = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!refreshTokenService.validateRefreshToken(rt)) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        String newAccessToken = jwtUtil.generateAccessToken(rt.getUser().getUsername(), 15 * 60 * 1000);
        return Map.of("accessToken", newAccessToken);
    }

    @PostMapping("/logout")
    public String logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        refreshTokenService.revokeToken(refreshToken);
        return "Logged out successfully";
    }
}
