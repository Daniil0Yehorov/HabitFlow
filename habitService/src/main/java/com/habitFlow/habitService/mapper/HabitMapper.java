package com.habitFlow.habitService.mapper;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.model.Habit;

public class HabitMapper {

    public static HabitDto toDto(Habit habit) {
        return HabitDto.builder()
                .id(habit.getId())
                .userId(habit.getUserId())
                .title(habit.getTitle())
                .description(habit.getDescription())
                .frequency(habit.getFrequency())
                .startDate(habit.getStartDate())
                .endDate(habit.getEndDate())
                .status(habit.getStatus())
                .build();
    }

    public static Habit toEntity(HabitDto dto) {
        return Habit.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .frequency(dto.getFrequency())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(dto.getStatus())
                .build();
    }
}