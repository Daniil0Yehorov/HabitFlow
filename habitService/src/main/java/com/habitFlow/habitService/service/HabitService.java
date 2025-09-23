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

    public HabitDto updateHabit(Long id, HabitDto dto) {
        return habitRepository.findById(id)
                .map(habit -> {
                    habit.setTitle(dto.getTitle());
                    habit.setDescription(dto.getDescription());
                    habit.setFrequency(dto.getFrequency());
                    habit.setStartDate(dto.getStartDate());
                    habit.setEndDate(dto.getEndDate());
                    habit.setStatus(dto.getStatus());
                    habit.setUpdatedAt(LocalDateTime.now());
                    return HabitMapper.toDto(habitRepository.save(habit));
                })
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));
    }

    public void deleteHabit(Long id) {
        if (!habitRepository.existsById(id)) {
            throw new ResourceNotFoundException("Habit not found with id: " + id);
        }
        habitRepository.deleteById(id);
    }
    public Optional<HabitDto> getHabit(Long id) {
        return habitRepository.findById(id).map(HabitMapper::toDto);
    }
    public List<HabitDto> getHabitsByUser(Long userId) {
        return habitRepository.findByUserId(userId).
                stream().map(HabitMapper::toDto)
                .toList();
    }
}