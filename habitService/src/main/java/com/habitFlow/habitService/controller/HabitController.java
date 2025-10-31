package com.habitFlow.habitService.controller;

import com.habitFlow.habitService.dto.HabitCreateDto;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.dto.HabitUpdateDto;
import com.habitFlow.habitService.service.HabitFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/habit")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Habit management", description = "Manage user habits (create, update, delete, view)")
public class HabitController {

    private final HabitFacade habitFacade;

    @Operation(summary = "Create a new habit", description = "Creates a new habit for the authenticated" +
            " user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habit created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid habit data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "Access forbidden"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "502", description = "Notification Service or User Service unavailable")
    })
    @PostMapping
    public ResponseEntity<HabitDto> createHabit(@Valid @RequestBody HabitCreateDto dto) {
        return ResponseEntity.ok(habitFacade.createHabit(dto));
    }

    @Operation(summary = "Get all habits of current user", description = "Returns all habits belonging to" +
            " the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of habits returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "502", description = "User Service unavailable")
    })
    @GetMapping("/me")
    public ResponseEntity<List<HabitDto>> getMyHabits() {
        return ResponseEntity.ok(habitFacade.getMyHabits());
    }

    @Operation(summary = "Get habit by ID", description = "Returns a specific habit by its ID for the" +
            " current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habit found successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid habit ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "Access to this habit is forbidden"),
            @ApiResponse(responseCode = "404", description = "Habit not found"),
            @ApiResponse(responseCode = "502", description = "User Service unavailable")
    })
    @GetMapping("/{id}")
    public ResponseEntity<HabitDto> getHabit(@PathVariable Long id) {
        return ResponseEntity.ok(habitFacade.getHabit(id));
    }

    @Operation(summary = "Update existing habit", description = "Updates a habit by ID for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Habit updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid habit ID format or malformed request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access — token is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "User has no permission to update this habit"),
            @ApiResponse(responseCode = "404", description = "Habit not found for given ID"),
            @ApiResponse(responseCode = "502", description = "External service (User/Notification) unavailable")
    })
    @PutMapping("/{id}")
    public ResponseEntity<HabitDto> updateHabit(@PathVariable Long id, @RequestBody HabitUpdateDto dto) {
        return ResponseEntity.ok(habitFacade.updateHabit(id, dto));
    }

    @Operation(summary = "Delete habit", description = "Deletes a habit by ID for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Habit deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid habit ID"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "403", description = "Access to this habit is forbidden"),
            @ApiResponse(responseCode = "404", description = "Habit not found"),
            @ApiResponse(responseCode = "502", description = "User Service unavailable")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHabit(@PathVariable Long id) {
        habitFacade.deleteHabit(id);
        return ResponseEntity.noContent().build();
    }

}