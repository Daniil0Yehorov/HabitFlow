package com.habitFlow.habitService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitFlow.habitService.config.JwtUtil;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.exception.ExternalServiceException;
import com.habitFlow.habitService.exception.ForbiddenException;
import com.habitFlow.habitService.exception.ResourceNotFoundException;
import com.habitFlow.habitService.model.enums.Frequency;
import com.habitFlow.habitService.model.enums.HabitStatus;
import com.habitFlow.habitService.service.HabitService;
import com.habitFlow.habitService.service.HabitTrackingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HabitController.class})
@Import({JwtUtil.class})
@AutoConfigureMockMvc(addFilters = false)
class HabitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HabitService habitService;

    @MockBean
    private HabitTrackingService trackingService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
        // Create a new habit successfully
    void createHabit_ReturnsHabit() throws Exception {
        HabitDto dto = HabitDto.builder()
                .id(1L)
                .title("New Habit")
                .frequency(Frequency.DAILY)
                .status(HabitStatus.ACTIVE)
                .build();

        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.createHabit(any(HabitDto.class), eq("user"))).thenReturn(dto);

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Habit"));
    }

    @Test
        // Get the list of habits for the authenticated user
    void getMyHabits_ReturnsList() throws Exception {
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.getHabitsByUsername("user"))
                .thenReturn(List.of(HabitDto.builder().title("Test").build()));

        mockMvc.perform(get("/habit/me")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test"));
    }

    @Test
        // Get a habit by ID that the user is forbidden to access
    void getHabit_Forbidden_Returns403() throws Exception {
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.getHabitByIdAndUsername(eq(1L), eq("user")))
                .thenThrow(new ForbiddenException("Access denied"));

        mockMvc.perform(get("/habit/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
        // Get a habit by ID that does not exist
    void getHabit_NotFound_Returns404() throws Exception {
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.getHabitByIdAndUsername(eq(1L), eq("user")))
                .thenThrow(new ResourceNotFoundException("Habit not found"));

        mockMvc.perform(get("/habit/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found"));
    }

    @Test
        // Create habit throws runtime exception
    void createHabit_RuntimeError_Returns400() throws Exception {
        HabitDto dto = HabitDto.builder().title("Test").build();

        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.createHabit(any(HabitDto.class), eq("user")))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unexpected error"));
    }

    @Test
        // Get habit throws external service exception
    void getHabit_ExternalServiceError_Returns502() throws Exception {
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.getHabitByIdAndUsername(eq(1L), eq("user")))
                .thenThrow(new ExternalServiceException("External service unavailable"));

        mockMvc.perform(get("/habit/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("External service unavailable"));
    }

    @Test
        // Update an existing habit successfully
    void updateHabit_Success_ReturnsUpdatedHabit() throws Exception {
        HabitDto dto = HabitDto.builder()
                .id(1L)
                .title("Updated Habit")
                .frequency(Frequency.WEEKLY)
                .status(HabitStatus.ACTIVE)
                .build();

        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.updateHabit(eq(1L), any(HabitDto.class), eq("user"))).thenReturn(dto);

        mockMvc.perform(put("/habit/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Habit"));
    }

    @Test
        // Update habit that does not exist
    void updateHabit_NotFound_Returns404() throws Exception {
        HabitDto dto = HabitDto.builder().title("Updated").build();

        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.updateHabit(eq(1L), any(HabitDto.class), eq("user")))
                .thenThrow(new ResourceNotFoundException("Habit not found"));

        mockMvc.perform(put("/habit/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found"));
    }

    @Test
        // Update habit forbidden
    void updateHabit_Forbidden_Returns403() throws Exception {
        HabitDto dto = HabitDto.builder().title("Updated").build();

        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(habitService.updateHabit(eq(1L), any(HabitDto.class), eq("user")))
                .thenThrow(new ForbiddenException("Access denied"));

        mockMvc.perform(put("/habit/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
        // Delete habit successfully
    void deleteHabit_Success_ReturnsNoContent() throws Exception {
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");

        mockMvc.perform(delete("/habit/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNoContent());

        Mockito.verify(habitService).deleteHabit(1L, "user");
    }

    @Test
        // Delete habit forbidden
    void deleteHabit_Forbidden_Returns403() throws Exception {
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.doThrow(new ForbiddenException("Access denied"))
                .when(habitService).deleteHabit(1L, "user");

        mockMvc.perform(delete("/habit/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }
}