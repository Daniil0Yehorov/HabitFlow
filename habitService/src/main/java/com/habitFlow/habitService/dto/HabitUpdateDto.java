package com.habitFlow.habitService.dto;

import com.habitFlow.habitService.model.enums.Frequency;
import com.habitFlow.habitService.model.enums.HabitStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@Schema(name = "HabitUpdateDto", description = "DTO for updating a habit (partial update)")
@AllArgsConstructor
@NoArgsConstructor
public class HabitUpdateDto {

    @Schema(description = "Title of the habit")
    private String title;

    @Schema(description = "Description of the habit")
    private String description;

    @Schema(description = "Frequency of the habit")
    private Frequency frequency;

    @Schema(description = "End date")
    private LocalDate endDate;

    @Schema(description = "Status of the habit")
    private HabitStatus status;
}