package com.habitFlow.habitService.controller;
import com.habitFlow.habitService.config.JwtUtil;
import com.habitFlow.habitService.dto.HabitTrackingDto;
import com.habitFlow.habitService.service.HabitTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class HabitTrackingController {

    private final HabitTrackingService trackingService;
    private final JwtUtil jwtUtil;

    @PostMapping("/habit/{habitId}")
    public ResponseEntity<HabitTrackingDto> createTracking(
            @PathVariable Long habitId,
            @RequestBody HabitTrackingDto dto) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(trackingService.createTracking(username, habitId, dto));
    }

    @GetMapping("/habit/{habitId}")
    public ResponseEntity<List<HabitTrackingDto>> getTrackingsByHabit(
            @PathVariable Long habitId) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(trackingService.getTrackingsByHabit(username, habitId));
    }

    @GetMapping("/habit/{habitId}/date/{date}")
    public ResponseEntity<List<HabitTrackingDto>> getTrackingByDate(
            @PathVariable Long habitId,
            @PathVariable String date) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(trackingService.getTrackingByDate(username,
                habitId, LocalDate.parse(date)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTracking(
            @PathVariable Long id) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        trackingService.deleteTracking(username, id);
        return ResponseEntity.noContent().build();
    }
}
