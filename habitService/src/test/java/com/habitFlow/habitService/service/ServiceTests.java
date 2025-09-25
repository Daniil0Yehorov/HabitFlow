package com.habitFlow.habitService.service;

import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.dto.HabitTrackingDto;
import com.habitFlow.habitService.exception.ForbiddenException;
import com.habitFlow.habitService.exception.ResourceNotFoundException;
import com.habitFlow.habitService.mapper.HabitMapper;
import com.habitFlow.habitService.mapper.HabitTrackingMapper;
import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.model.HabitTracking;
import com.habitFlow.habitService.model.enums.Frequency;
import com.habitFlow.habitService.model.enums.HabitStatus;
import com.habitFlow.habitService.repository.HabitRepository;
import com.habitFlow.habitService.repository.HabitTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ServiceTests {
    @Mock
    private HabitRepository habitRepository;

    @Mock
    private HabitTrackingRepository trackingRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private HabitService habitService;

    @InjectMocks
    private HabitTrackingService trackingService;

    private Habit habit;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        habit = Habit.builder()
                .id(1L)
                .userId(100L)
                .title("Test Habit")
                .description("Description")
                .frequency(Frequency.DAILY)
                .status(HabitStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ---------- HabitService Tests ----------

    @Test
        // Create a habit successfully
    void createHabit_Success() {
        HabitDto dto = HabitMapper.toDto(habit);
        when(userService.getUserIdByUsername("user")).thenReturn(100L);
        when(habitRepository.save(any(Habit.class))).thenReturn(habit);

        HabitDto result = habitService.createHabit(dto, "user");

        assertThat(result.getTitle()).isEqualTo("Test Habit");
        verify(habitRepository, times(1)).save(any(Habit.class));
    }

    @Test
        // Get habit by ID, user forbidden
    void getHabitByIdAndUsername_Forbidden() {
        when(userService.getUserIdByUsername("user")).thenReturn(200L);
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));

        assertThrows(ForbiddenException.class,
                () -> habitService.getHabitByIdAndUsername(1L, "user"));
    }

    @Test
        // Update habit that does not exist
    void updateHabit_NotFound() {
        when(userService.getUserIdByUsername("user")).thenReturn(100L);
        when(habitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> habitService.updateHabit(1L, HabitMapper.toDto(habit), "user"));
    }

    @Test
        // Delete habit successfully
    void deleteHabit_Success() {
        when(userService.getUserIdByUsername("user")).thenReturn(100L);
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));

        habitService.deleteHabit(1L, "user");

        verify(habitRepository, times(1)).delete(habit);
    }

    // ---------- HabitTrackingService Tests ----------

    @Test
        // Create tracking successfully
    void createTracking_Success() {
        HabitTrackingDto dto = HabitTrackingDto.builder()
                .trackDate(LocalDate.now())
                .done(true)
                .build();

        when(userService.getUserIdByUsername("user")).thenReturn(100L);
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
        when(trackingRepository.save(any(HabitTracking.class)))
                .thenReturn(HabitTrackingMapper.toEntity(dto));

        HabitTrackingDto result = trackingService.createTracking("user", 1L, dto);

        assertThat(result.isDone()).isTrue();
    }

    @Test
        // Get trackings by habit, user forbidden
    void getTrackingsByHabit_Forbidden() {
        when(userService.getUserIdByUsername("user")).thenReturn(200L);
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));

        assertThrows(ForbiddenException.class,
                () -> trackingService.getTrackingsByHabit("user", 1L));
    }

    @Test
        // Delete tracking that does not exist
    void deleteTracking_NotFound() {
        when(userService.getUserIdByUsername("user")).thenReturn(100L);
        when(trackingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> trackingService.deleteTracking("user", 1L));
    }

    @Test
        // Get tracking by date successfully
    void getTrackingByDate_Success() {
        LocalDate date = LocalDate.now();
        HabitTrackingDto dto = HabitTrackingDto.builder().trackDate(date).done(true).build();
        HabitTracking tracking = HabitTrackingMapper.toEntity(dto);
        tracking.setHabit(habit);

        when(userService.getUserIdByUsername("user")).thenReturn(100L);
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
        when(trackingRepository.findByHabitIdAndTrackDate(1L, date))
                .thenReturn(List.of(tracking));

        List<HabitTrackingDto> result = trackingService.getTrackingByDate("user", 1L, date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isDone()).isTrue();
    }

    @Test
        // Get tracking by date forbidden
    void getTrackingByDate_Forbidden() {
        LocalDate date = LocalDate.now();
        when(userService.getUserIdByUsername("user")).thenReturn(200L);
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));

        assertThrows(ForbiddenException.class,
                () -> trackingService.getTrackingByDate("user", 1L, date));
    }
}