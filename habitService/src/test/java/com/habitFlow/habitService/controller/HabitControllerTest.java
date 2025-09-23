package com.habitFlow.habitService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.service.HabitService;
import com.habitFlow.habitService.config.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HabitController.class)
@Import({JwtUtil.class})
@AutoConfigureMockMvc(addFilters = false)
class HabitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HabitService habitService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void createHabit_ReturnsHabit() throws Exception {
        HabitDto dto = HabitDto.builder().id(1L).title("Test").build();
        Mockito.when(jwtUtil.extractUsername(anyString())).thenReturn("john");
        Mockito.when(habitService.createHabit(any(HabitDto.class), anyString())).thenReturn(dto);

        mockMvc.perform(post("/habit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test"));
    }

    @Test
    void getHabit_ReturnsHabit() throws Exception {
        HabitDto dto = HabitDto.builder().id(1L).title("Test").build();
        Mockito.when(habitService.getHabit(1L)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/habit/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getHabit_NotFound() throws Exception {
        Mockito.when(habitService.getHabit(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/habit/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getHabitsByUser_ReturnsList() throws Exception {
        HabitDto dto = HabitDto.builder().id(1L).title("Test").build();
        Mockito.when(habitService.getHabitsByUser(1L)).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/habit/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void updateHabit_ReturnsUpdatedHabit() throws Exception {
        HabitDto dto = HabitDto.builder().id(1L).title("Updated").build();
        Mockito.when(habitService.updateHabit(eq(1L), any(HabitDto.class))).thenReturn(dto);

        mockMvc.perform(put("/habit/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void deleteHabit_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/habit/1"))
                .andExpect(status().isNoContent());

        Mockito.verify(habitService).deleteHabit(1L);
    }
}