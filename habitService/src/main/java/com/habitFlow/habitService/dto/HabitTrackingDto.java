package com.habitFlow.habitService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitTrackingDto {
    private Long id;
    private LocalDate trackDate;
    private boolean done;
}
