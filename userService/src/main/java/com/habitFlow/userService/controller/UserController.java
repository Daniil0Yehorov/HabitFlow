package com.habitFlow.userService.controller;

import com.habitFlow.userService.dto.UserDto;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.config.JwtUtil;
import com.habitFlow.userService.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    //useless mb
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userService.findByUsername(username);
        if (user == null) return ResponseEntity.notFound().build();

        return ResponseEntity.ok(new UserDto(user.getId(), user.getUsername(), user.getEmail()));
    }
    //change stats about yourself "add later"

}