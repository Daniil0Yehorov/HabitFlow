package com.habitFlow.habitService.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "HabitTrackingDto", description = "Represents a single tracking record for a habit")
public class HabitTrackingDto {
    @Schema(description = "Unique tracking record ID", example = "1")
    private Long id;

    @NotNull(message = "Track date must not be null")
    @Schema(description = "Date when the habit was tracked", example = "2025-10-03")
    private LocalDate trackDate;

    @Schema(description = "Indicates if the habit was completed on this date", example = "true")
    private boolean done;
}