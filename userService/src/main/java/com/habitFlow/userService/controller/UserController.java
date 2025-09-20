package com.habitFlow.userService.controller;

import com.habitFlow.userService.model.User;
import com.habitFlow.userService.service.JwtUtil;
import com.habitFlow.userService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @GetMapping("/me")
    public User getCurrentUser(@RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        if (jwtUtil.isTokenExpired(token)) {
            throw new RuntimeException("Token expired");
        }

        String username = jwtUtil.extractUsername(token);
        return userService.findByUsername(username);
    }
}