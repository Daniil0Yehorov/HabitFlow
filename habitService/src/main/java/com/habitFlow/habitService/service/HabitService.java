package com.habitFlow.habitService.service;

import com.habitFlow.habitService.config.NotificationClient;
import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.HabitCreateDto;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.dto.HabitUpdateDto;
import com.habitFlow.habitService.dto.UserDto;
import com.habitFlow.habitService.exception.custom.ForbiddenException;
import com.habitFlow.habitService.exception.custom.ResourceNotFoundException;
import com.habitFlow.habitService.mapper.HabitMapper;
import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.model.HabitTracking;
import com.habitFlow.habitService.repository.HabitRepository;
import com.habitFlow.habitService.repository.HabitTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserService userService;
    private final NotificationClient notificationClient;
    private final HabitTrackingRepository habitTrackingRepository;

    public HabitDto createHabit(HabitCreateDto dto, Long userId, String username) {
        Habit habit = HabitMapper.ToEntity(dto);
        habit.setUserId(userId);
        habit.setCreatedAt(LocalDateTime.now());
        habit.setUpdatedAt(LocalDateTime.now());

        Habit saved = habitRepository.save(habit);

        notificationClient.dispatchNotification(
                username,
                "Habit Created",
                "Your Habit '" + dto.getTitle() + "' created successfully."
        );

        return HabitMapper.toDto(saved);
    }
    public List<HabitDto> getHabitsByUserId(Long userId) {
        return habitRepository.findByUserId(userId)
                .stream()
                .map(HabitMapper::toDto)
                .toList();
    }

    public HabitDto getHabitById(Long id, Long userId) {
        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        if (!habit.getUserId().equals(userId)) {
            throw new ForbiddenException("You don’t have access to this habit");
        }

        return HabitMapper.toDto(habit);
    }

    public List<HabitDto> getHabitsByUsername(String username) {
        UserDto dto = userService.getUserByUsername(username);

        return habitRepository.findByUserId(dto.getId())
                .stream()
                .map(HabitMapper::toDto)
                .toList();
    }

    public HabitDto getHabitByIdAndUsername(Long id, String username) {
        UserDto userdto = userService.getUserByUsername(username);

        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        if (!habit.getUserId().equals(userdto.getId())) {
            throw new ForbiddenException("You don’t have access to this habit");
        }

        return HabitMapper.toDto(habit);
    }

    public HabitDto updateHabit(Long id, HabitUpdateDto dto, Long userId, String username) {
        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        if (!habit.getUserId().equals(userId)) {
            throw new ForbiddenException("You don’t have access to this habit");
        }

        if (dto.getTitle() != null) habit.setTitle(dto.getTitle());
        if (dto.getDescription() != null) habit.setDescription(dto.getDescription());
        if (dto.getFrequency() != null) habit.setFrequency(dto.getFrequency());
        if (dto.getEndDate() != null) habit.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) habit.setStatus(dto.getStatus());

        habit.setUpdatedAt(LocalDateTime.now());
        Habit updated = habitRepository.save(habit);

        notificationClient.dispatchNotification(
                username,
                "Habit Updated","Your Habit '" + habit.getTitle() + "' was updated."
        );

        return HabitMapper.toDto(updated);
    }

    public void deleteHabit(Long id, String username) {
        UserDto userdto = userService.getUserByUsername(username);

        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        if (!habit.getUserId().equals(userdto.getId())) {
            throw new ForbiddenException("You don’t have access to delete this habit");
        }

        habitRepository.delete(habit);
    }

    @Transactional
    public void deleteHabitByIdWithTrackings(Long habitId) {
        List<HabitTracking> trackings = habitTrackingRepository.findByHabitId(habitId);
        if (!trackings.isEmpty()) {
            habitTrackingRepository.deleteAll(trackings);
        }

        habitRepository.deleteById(habitId);
    }

    public List<Habit> findBatchOfHabits(Long lastId, int limit) {
        return habitRepository.findTopNByIdGreaterThanOrderByIdAsc(lastId, limit);
    }

    public void deleteHabit(Long id, Long userId) {
        Habit habit = habitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Habit not found with id: " + id));

        if (!habit.getUserId().equals(userId)) {
            throw new ForbiddenException("You don’t have access to delete this habit");
        }

        habitRepository.delete(habit);
    }
}