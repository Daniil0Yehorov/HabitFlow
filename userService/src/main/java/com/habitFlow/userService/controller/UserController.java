package com.habitFlow.userService.controller;

import com.habitFlow.userService.dto.UserDto;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.service.JwtUtil;
import com.habitFlow.userService.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    //useless mb
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        User user = userService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok(new UserDto(user.getId(),user.getUsername(), user.getEmail()));
    }

}