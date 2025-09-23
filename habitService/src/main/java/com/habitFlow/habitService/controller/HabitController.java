package com.habitFlow.habitService.controller;

import com.habitFlow.habitService.config.JwtUtil;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.service.HabitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/habit")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    private final JwtUtil jwtUtil;
    @PostMapping
    public ResponseEntity<HabitDto> createHabit(
            @RequestBody HabitDto habitDto,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        System.out.println("User '" + username + "' is creating a habit: " + habitDto.getTitle());

        return ResponseEntity.ok(habitService.createHabit(habitDto, username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitDto> getHabit(@PathVariable Long id) {
        return habitService.getHabit(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<HabitDto>> getHabitsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(habitService.getHabitsByUser(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitDto> updateHabit(@PathVariable Long id, @RequestBody HabitDto habitDto) {
        return ResponseEntity.ok(habitService.updateHabit(id, habitDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabit(@PathVariable Long id) {
        habitService.deleteHabit(id);
        return ResponseEntity.noContent().build();
    }

}