package com.habitFlow.habitService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitFlow.habitService.config.JwtUtil;
import com.habitFlow.habitService.dto.HabitTrackingDto;
import com.habitFlow.habitService.exception.ResourceNotFoundException;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HabitTrackingController.class})
@Import({JwtUtil.class})
@AutoConfigureMockMvc(addFilters = false)
public class HabitTrackingControllerTest {
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
        // Create tracking entry successfully
    void createTracking_ReturnsTracking() throws Exception {
        HabitTrackingDto dto = HabitTrackingDto.builder()
                .id(1L)
                .trackDate(LocalDate.now())
                .done(true)
                .build();

        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(trackingService.createTracking(eq("user"), eq(1L), any(HabitTrackingDto.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/tracking/habit/1")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));
    }

    @Test
        // Get all trackings for a habit
    void getTrackingsByHabit_ReturnsList() throws Exception {
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(trackingService.getTrackingsByHabit("user", 1L))
                .thenReturn(List.of(HabitTrackingDto.builder().done(true).build()));

        mockMvc.perform(get("/tracking/habit/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].done").value(true));
    }

    @Test
        // Get trackings by date successfully
    void getTrackingByDate_ReturnsList() throws Exception {
        LocalDate date = LocalDate.now();
        HabitTrackingDto dto = HabitTrackingDto.builder()
                .trackDate(date)
                .done(true)
                .build();

        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(trackingService.getTrackingByDate("user", 1L, date))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/tracking/habit/1/date/" + date.toString())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].done").value(true));
    }

    @Test
        // Get trackings by date that do not exist
    void getTrackingByDate_NotFound_Returns404() throws Exception {
        LocalDate date = LocalDate.now();

        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.when(trackingService.getTrackingByDate("user", 1L, date))
                .thenThrow(new ResourceNotFoundException("No tracking found"));

        mockMvc.perform(get("/tracking/habit/1/date/" + date.toString())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No tracking found"));
    }

    @Test
        // Delete tracking entry that does not exist
    void deleteTracking_NotFound_Returns404() throws Exception {
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("user");
        Mockito.doThrow(new ResourceNotFoundException("Tracking not found"))
                .when(trackingService).deleteTracking(eq("user"), eq(1L));

        mockMvc.perform(delete("/tracking/1")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tracking not found"));
    }
}
