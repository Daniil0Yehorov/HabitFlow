package com.habitFlow.habitService.service;

import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.HabitTrackingDto;
import com.habitFlow.habitService.exception.ForbiddenException;
import com.habitFlow.habitService.exception.ResourceNotFoundException;
import com.habitFlow.habitService.mapper.HabitTrackingMapper;
import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.model.HabitTracking;
import com.habitFlow.habitService.repository.HabitRepository;
import com.habitFlow.habitService.repository.HabitTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitTrackingService {

    private final HabitTrackingRepository habitTrackingRepository;
    private final HabitRepository habitRepository;

    private final UserService userService;

    public HabitTrackingDto createTracking(String username, Long habitId, HabitTrackingDto dto) {
        Long userId = userService.getUserIdByUsername(username);

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + habitId));

        if (!habit.getUserId().equals(userId)) {
            throw new ForbiddenException("You cannot add tracking for this habit");
        }

        HabitTracking tracking = HabitTrackingMapper.toEntity(dto);
        tracking.setHabit(habit);

        return HabitTrackingMapper.toDto(habitTrackingRepository.save(tracking));
    }

    public List<HabitTrackingDto> getTrackingsByHabit(String username, Long habitId) {
        Long userId = userService.getUserIdByUsername(username);

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + habitId));

        if (!habit.getUserId().equals(userId)) {
            throw new ForbiddenException("You cannot view trackings of this habit");
        }

        return habitTrackingRepository.findByHabitId(habitId).stream()
                .map(HabitTrackingMapper::toDto)
                .toList();
    }

    public List<HabitTrackingDto> getTrackingByDate(String username, Long habitId, LocalDate date) {
        Long userId = userService.getUserIdByUsername(username);

        Habit habit = habitRepository.findById(habitId)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + habitId));

        if (!habit.getUserId().equals(userId)) {
            throw new ForbiddenException("You cannot view tracking of this habit");
        }

        return habitTrackingRepository.findByHabitIdAndTrackDate(habitId, date).stream()
                .map(HabitTrackingMapper::toDto)
                .toList();
    }

    public void deleteTracking(String username, Long id) {
        Long userId = userService.getUserIdByUsername(username);

        HabitTracking tracking = habitTrackingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HabitTracking not found with id: " + id));

        if (!tracking.getHabit().getUserId().equals(userId)) {
            throw new ForbiddenException("You cannot delete this tracking");
        }

        habitTrackingRepository.delete(tracking);
    }
}
