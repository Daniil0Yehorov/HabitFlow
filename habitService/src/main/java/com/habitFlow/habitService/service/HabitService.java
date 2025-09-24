package com.habitFlow.habitService.service;

import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.exception.ResourceNotFoundException;
import com.habitFlow.habitService.mapper.HabitMapper;
import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserService userService;

    public HabitDto createHabit(HabitDto dto, String username) {
        Long userId = userService.getUserIdByUsername(username);
        if (userId == null) {
            throw new ResourceNotFoundException("User not found: " + username);
        }

        Habit habit = HabitMapper.toEntity(dto);
        habit.setUserId(userId);
        habit.setCreatedAt(LocalDateTime.now());
        habit.setUpdatedAt(LocalDateTime.now());

        return HabitMapper.toDto(habitRepository.save(habit));
    }

    public List<HabitDto> getHabitsByUsername(String username) {
        Long userId = userService.getUserIdByUsername(username);

        return habitRepository.findByUserId(userId)
                .stream()
                .map(HabitMapper::toDto)
                .toList();
    }

    public HabitDto getHabitByIdAndUsername(Long id, String username) {
        Long userId = userService.getUserIdByUsername(username);

        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found"));

        if (!habit.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("You don’t have access to this habit");
        }

        return HabitMapper.toDto(habit);
    }

    public HabitDto updateHabit(Long id, HabitDto dto, String username) {
        Long userId = userService.getUserIdByUsername(username);
        if (userId == null) {
            throw new ResourceNotFoundException("User not found: " + username);
        }

        return habitRepository.findById(id)
                .map(habit -> {
                    if (!habit.getUserId().equals(userId)) {
                        throw new ResourceNotFoundException("Habit not found for this user: " + id);
                    }

                    if (dto.getTitle() != null) habit.setTitle(dto.getTitle());
                    if (dto.getDescription() != null) habit.setDescription(dto.getDescription());
                    if (dto.getFrequency() != null) habit.setFrequency(dto.getFrequency());
                    if (dto.getStartDate() != null) habit.setStartDate(dto.getStartDate());
                    if (dto.getEndDate() != null) habit.setEndDate(dto.getEndDate());
                    if (dto.getStatus() != null) habit.setStatus(dto.getStatus());

                    habit.setUpdatedAt(LocalDateTime.now());

                    return HabitMapper.toDto(habitRepository.save(habit));
                })
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));
    }

    public void deleteHabit(Long id, String username) {
        Long userId = userService.getUserIdByUsername(username);

        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found"));

        if (!habit.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("You don’t have access to this habit");
        }

        habitRepository.delete(habit);
    }
}