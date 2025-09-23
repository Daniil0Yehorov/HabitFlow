package com.habitFlow.habitService.service;

import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.exception.ResourceNotFoundException;
import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.repository.HabitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HabitServiceTest {

    private HabitRepository habitRepository;
    private UserService userService;
    private HabitService habitService;

    @BeforeEach
    void setUp() {
        habitRepository = mock(HabitRepository.class);
        userService = mock(UserService.class);
        habitService = new HabitService(habitRepository, userService);
    }

    @Test
    void createHabit_UserNotFound_ThrowsException() {
        when(userService.getUserIdByUsername("john")).thenReturn(null);
        HabitDto dto = HabitDto.builder().title("Test").build();

        assertThrows(ResourceNotFoundException.class, () -> habitService.createHabit(dto, "john"));
    }

    @Test
    void createHabit_Valid_ReturnsHabit() {
        when(userService.getUserIdByUsername("john")).thenReturn(1L);
        HabitDto dto = HabitDto.builder().title("Test").build();
        Habit saved = new Habit();
        saved.setId(1L);
        saved.setTitle("Test");

        when(habitRepository.save(any())).thenReturn(saved);

        HabitDto result = habitService.createHabit(dto, "john");

        assertEquals(1L, result.getId());
        assertEquals("Test", result.getTitle());
        verify(habitRepository).save(any());
    }

    @Test
    void updateHabit_HabitNotFound_ThrowsException() {
        when(habitRepository.findById(1L)).thenReturn(Optional.empty());
        HabitDto dto = HabitDto.builder().title("Test").build();

        assertThrows(ResourceNotFoundException.class, () -> habitService.updateHabit(1L, dto));
    }

    @Test
    void updateHabit_Valid_ReturnsUpdatedHabit() {
        Habit habit = new Habit();
        habit.setId(1L);
        habit.setTitle("Old");
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
        when(habitRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        HabitDto dto = HabitDto.builder().title("New").build();
        HabitDto updated = habitService.updateHabit(1L, dto);

        assertEquals("New", updated.getTitle());
    }

    @Test
    void deleteHabit_HabitNotFound_ThrowsException() {
        when(habitRepository.existsById(1L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> habitService.deleteHabit(1L));
    }

    @Test
    void deleteHabit_Valid_DeletesHabit() {
        when(habitRepository.existsById(1L)).thenReturn(true);
        habitService.deleteHabit(1L);
        verify(habitRepository).deleteById(1L);
    }

    @Test
    void getHabit_ReturnsHabit() {
        Habit habit = new Habit();
        habit.setId(1L);
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));

        Optional<HabitDto> result = habitService.getHabit(1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void getHabitsByUser_ReturnsList() {
        Habit habit = new Habit();
        habit.setId(1L);
        when(habitRepository.findByUserId(1L)).thenReturn(List.of(habit));

        List<HabitDto> list = habitService.getHabitsByUser(1L);
        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getId());
    }
}
