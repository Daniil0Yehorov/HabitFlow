package com.habitFlow.userService.service;
import com.habitFlow.userService.config.JwtUtil;
import com.habitFlow.userService.config.NotificationClient;
import com.habitFlow.userService.dto.*;
import com.habitFlow.userService.exception.custom.*;
import com.habitFlow.userService.model.RefreshToken;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationClient notificationClient;

    @Value("${LinkForVerify}")
    private String linkForVerify;

    public String register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent())
            throw new DuplicateUserException("Username already exists");

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new DuplicateUserException("Email already registered");

        String verificationToken = UUID.randomUUID().toString().substring(0, 6);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .emailVerified(false)
                .verificationCode(verificationToken)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        notificationClient.createInitialNotificationSettings(new NotificationSettingsRequest(
                user.getId(), user.getUsername(), user.getEmail()
        ));

        String verificationLink = linkForVerify + user.getEmail() + "&token=" + verificationToken;
        String message = String.format(
                "Hi %s!\n\nClick the link below to verify your email:\n%s",
                user.getUsername(), verificationLink
        );

        notificationClient.sendVerificationEmail(user.getEmail(), "HabitFlow Email Verification",
                message);
        return "User registered! Please check your email for the verification code.";
    }

    public AuthResponse login(LoginRequest request) {
        User found = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!found.isEmailVerified())
            throw new UnverifiedEmailException("Please verify your email before logging in");

        if (!passwordEncoder.matches(request.getPassword(), found.getPassword()))
            throw new InvalidCredentialsException("Invalid credentials");

        String accessToken = jwtUtil.generateAccessToken(found.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(found);
        return new AuthResponse(accessToken, refreshToken.getToken());
    }

    public AuthResponse refresh(String refreshTokenStr) {
        RefreshToken rt = refreshTokenService.findByToken(refreshTokenStr)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!refreshTokenService.validateRefreshToken(rt))
            throw new InvalidTokenException("Refresh token expired");

        String newAccessToken = jwtUtil.generateAccessToken(rt.getUser().getUsername());
        return new AuthResponse(newAccessToken, refreshTokenStr);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    public String verifyEmail(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!token.equals(user.getVerificationCode()))
            throw new InvalidTokenException("Invalid or expired verification token");

        user.setEmailVerified(true);
        user.setVerificationCode(null);
        userRepository.save(user);

        notificationClient.confirmEmailChannel(new NotificationSettingsRequest(
                user.getId(), user.getUsername(), user.getEmail()
        ));

        return "Email verified successfully! You can now log in.";
    }
}
