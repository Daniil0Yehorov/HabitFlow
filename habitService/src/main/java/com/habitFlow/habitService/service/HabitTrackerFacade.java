package com.habitFlow.habitService.service;

import com.habitFlow.habitService.dto.HabitTrackingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitTrackerFacade {
    private final HabitTrackingService trackingService;

    private String getUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public HabitTrackingDto createTracking(Long habitId, HabitTrackingDto dto) {
        String username = getUsername();
        return trackingService.createTracking(username, habitId, dto);
    }

    public List<HabitTrackingDto> getTrackingsByHabit(Long habitId) {
        String username = getUsername();
        return trackingService.getTrackingsByHabit(username, habitId);
    }

    public List<HabitTrackingDto> getTrackingByDate(Long habitId, LocalDate date) {
        String username = getUsername();
        return trackingService.getTrackingByDate(username, habitId, date);
    }

    public void deleteTracking(Long trackingId) {
        String username = getUsername();
        trackingService.deleteTracking(username, trackingId);
    }
}
