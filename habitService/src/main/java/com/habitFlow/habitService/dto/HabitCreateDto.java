package com.habitFlow.habitService.dto;

import com.habitFlow.habitService.model.enums.Frequency;
import com.habitFlow.habitService.model.enums.HabitStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@Schema(name = "HabitCreateDto", description = "DTO for creating a new habit")
@AllArgsConstructor
@NoArgsConstructor
public class HabitCreateDto {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 100, message = "Title length must be <= 100")
    private String title;

    @Size(max = 500, message = "Description max length is 500")
    private String description;

    @NotNull(message = "Frequency is required")
    private Frequency frequency;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Status is required")
    private HabitStatus status;
}