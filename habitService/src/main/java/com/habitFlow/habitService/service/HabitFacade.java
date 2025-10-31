package com.habitFlow.habitService.service;

import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.HabitCreateDto;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.dto.HabitUpdateDto;
import com.habitFlow.habitService.dto.UserDto;
import com.habitFlow.habitService.exception.custom.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HabitFacade {

    private final HabitService habitService;
    private final UserService userService;

    private String getUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Long getUserId() {
        String username = getUsername();
        UserDto user = userService.getUserByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("User not found: " + username);
        }
        return userService.getUserByUsername(username).getId();
    }

    public HabitDto createHabit(HabitCreateDto dto) {
        String username = getUsername();
        Long userId = getUserId();
        return habitService.createHabit(dto, userId, username);
    }

    public List<HabitDto> getMyHabits() {
        Long userId = getUserId();
        return habitService.getHabitsByUserId(userId);
    }

    public HabitDto getHabit(Long id) {
        Long userId = getUserId();
        return habitService.getHabitById(id, userId);
    }

    public HabitDto updateHabit(Long id, HabitUpdateDto dto) {
        String username = getUsername();
        Long userId = getUserId();
        return habitService.updateHabit(id, dto, userId, username);
    }

    public void deleteHabit(Long id) {
        Long userId = getUserId();
        habitService.deleteHabit(id, userId);
    }
}