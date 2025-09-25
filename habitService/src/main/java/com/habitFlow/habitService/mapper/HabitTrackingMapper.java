package com.habitFlow.habitService.mapper;

import com.habitFlow.habitService.dto.HabitTrackingDto;
import com.habitFlow.habitService.model.HabitTracking;

public class HabitTrackingMapper {

    public static HabitTrackingDto toDto(HabitTracking entity) {
        return HabitTrackingDto.builder()
                .id(entity.getId())
                .trackDate(entity.getTrackDate())
                .done(entity.isDone())
                .build();
    }

    public static HabitTracking toEntity(HabitTrackingDto dto) {
        return HabitTracking.builder()
                .id(dto.getId())
                .trackDate(dto.getTrackDate())
                .done(dto.isDone())
                .build();
    }
}
