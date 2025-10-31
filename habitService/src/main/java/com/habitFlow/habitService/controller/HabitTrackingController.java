package com.habitFlow.habitService.controller;

import com.habitFlow.habitService.dto.HabitTrackingDto;
import com.habitFlow.habitService.service.HabitTrackerFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Habit tracking", description = "Track user habits by date and completion")
public class HabitTrackingController {

    private  final HabitTrackerFacade trackingFacade;

    @Operation(summary = "Create tracking record", description = "Creates a tracking record for the given habit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tracking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date format or data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "User has no access to this habit"),
            @ApiResponse(responseCode = "404", description = "Habit not found"),
            @ApiResponse(responseCode = "502", description = "Notification Service unavailable")
    })
    @PostMapping("/habit/{habitId}")
    public ResponseEntity<HabitTrackingDto> createTracking(
            @Parameter(description = "Habit ID", required = true) @PathVariable Long habitId,
            @Valid @RequestBody HabitTrackingDto dto) {

        return ResponseEntity.ok(trackingFacade.createTracking(habitId, dto));
    }

    @Operation(summary = "Get all trackings for habit", description = "Returns all tracking records for" +
            " a specific habit")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trackings returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid habit ID parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "User has no access to this habit"),
            @ApiResponse(responseCode = "404", description = "Habit not found")
    })
    @GetMapping("/habit/{habitId}")
    public ResponseEntity<List<HabitTrackingDto>> getTrackingsByHabit(
            @Parameter(description = "Habit ID", required = true) @PathVariable Long habitId) {

        return ResponseEntity.ok(trackingFacade.getTrackingsByHabit(habitId));
    }

    @Operation(summary = "Get tracking by date", description = "Returns tracking records for a habit on" +
            " a specific date")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trackings returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "User has no access to this habit"),
            @ApiResponse(responseCode = "404", description = "Habit not found")
    })
    @GetMapping("/habit/{habitId}/date/{date}")
    public ResponseEntity<List<HabitTrackingDto>> getTrackingByDate(
            @Parameter(description = "Habit ID", required = true)
            @PathVariable Long habitId,
            @Parameter(description = "Tracking date (format: YYYY-MM-DD)",
                    required = true) @PathVariable String date) {

        return ResponseEntity.ok(trackingFacade.getTrackingByDate(habitId, LocalDate.parse(date)));
    }

    @Operation(summary = "Delete tracking record", description = "Deletes a specific tracking record by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tracking deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid tracking ID parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "User has no access to this tracking"),
            @ApiResponse(responseCode = "404", description = "Tracking not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTracking(@Parameter(description = "Tracking record ID",
            required = true) @PathVariable Long id) {

        trackingFacade.deleteTracking(id);
        return ResponseEntity.noContent().build();
    }
}
