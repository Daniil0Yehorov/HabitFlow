package com.habitFlow.habitService.controller;

import com.habitFlow.habitService.config.JwtUtil;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.service.HabitService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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
            @RequestBody HabitDto habitDto) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(habitService.createHabit(habitDto, username));
    }

    @GetMapping("/me")
    public ResponseEntity<List<HabitDto>> getMyHabits() {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(habitService.getHabitsByUsername(username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitDto> getHabit(
            @PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return ResponseEntity.ok(habitService.getHabitByIdAndUsername(id, username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitDto> updateHabit(
            @PathVariable Long id,
            @RequestBody HabitDto habitDto) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(habitService.updateHabit(id, habitDto, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabit(
            @PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        habitService.deleteHabit(id, username);
        return ResponseEntity.noContent().build();
    }

}