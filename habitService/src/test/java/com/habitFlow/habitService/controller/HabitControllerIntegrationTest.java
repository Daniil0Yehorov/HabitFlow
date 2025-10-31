package com.habitFlow.habitService.controller;

import com.habitFlow.habitService.config.JwtUtil;
import com.habitFlow.habitService.config.NotificationClient;
import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.HabitCreateDto;
import com.habitFlow.habitService.dto.HabitDto;
import com.habitFlow.habitService.dto.HabitUpdateDto;
import com.habitFlow.habitService.dto.UserDto;
import com.habitFlow.habitService.exception.custom.ExternalServiceException;
import com.habitFlow.habitService.exception.custom.ForbiddenException;
import com.habitFlow.habitService.model.enums.Frequency;
import com.habitFlow.habitService.model.enums.HabitStatus;
import com.habitFlow.habitService.repository.HabitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitFlow.habitService.service.HabitFacade;
import com.habitFlow.habitService.service.HabitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class HabitControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitService habitService;

    @Autowired
    private HabitFacade habitFacade;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationClient notificationClient;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private UserService userService;

    private String token1;
    private String token2;

    private UserDto testUser1;

    private UserDto testUser2;

    @BeforeEach
    void setup() {
        testUser1 = new UserDto();
        testUser1.setId(1L);
        testUser1.setUsername("testUser1");

        testUser2 = new UserDto();
        testUser2.setId(2L);
        testUser2.setUsername("testUser2");

        token1 = jwtUtil.generateAccessToken("testUser1");
        token2 = jwtUtil.generateAccessToken("testUser2");

        habitRepository.deleteAll();

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);
        Mockito.when(userService.getUserByUsername("testUser2")).thenReturn(testUser2);
    }

    // ================= CREATE HABIT (POST /habit/) =================

    @Test
    @DisplayName("✅ createHabit — 200 OK: valid data and authorized user")
    void createHabit_Success() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Morning Run");
        dto.setDescription("Run 3 km every morning");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(30));
        dto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Morning Run"))
                .andExpect(jsonPath("$.description").value("Run 3 km every morning"))
                .andExpect(jsonPath("$.frequency").value("DAILY"));

        Mockito.verify(notificationClient)
                .dispatchNotification(eq("testUser1"), contains("Morning Run"), eq("Habit Created"));
    }

    @Test
    @DisplayName("❌ createHabit — 400 BAD REQUEST: invalid data")
    void createHabit_BadRequest() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.title").value("Title cannot be blank"))
                .andExpect(jsonPath("$.fields.frequency").value("Frequency is required"))
                .andExpect(jsonPath("$.fields.startDate").value("Start date is required"))
                .andExpect(jsonPath("$.fields.status").value("Status is required"));
    }

    @Test
    @DisplayName("❌ createHabit — 401 UNAUTHORIZED: no token")
    void createHabit_Unauthorized() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Meditation");
        dto.setDescription("10 minutes of meditation");
        dto.setFrequency(Frequency.DAILY);
        dto.setEndDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/habit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ createHabit — 403 FORBIDDEN: userService throws exception")
    void createHabit_Forbidden() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Reading");
        dto.setDescription("Read for 30 minutes");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(15));
        dto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1"))
                .thenThrow(new ForbiddenException("Access denied"));

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    @DisplayName("❌ createHabit — 404 NOT FOUND: user not found")
    void createHabit_UserNotFound() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Yoga");
        dto.setDescription("Stretch every morning");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(20));
        dto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1"))
                .thenReturn(null);

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: testUser1"));
    }

    @Test
    @DisplayName("❌ createHabit — 502 BAD GATEWAY: Notification Service unavailable")
    void createHabit_NotificationServiceUnavailable() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Test Habit");
        dto.setDescription("Description");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        Mockito.doThrow(new ExternalServiceException("[NotificationClient] Notification Service unavailable"))
                .when(notificationClient).dispatchNotification(any(), any(), any());

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[NotificationClient] Notification" +
                        " Service unavailable"));
    }

    @Test
    @DisplayName("❌ createHabit — 502 BAD GATEWAY: User Service unavailable")
    void createHabit_UserServiceUnavailable() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Habit from user");
        dto.setDescription("Test description");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1"))
                .thenThrow(new ExternalServiceException("[UserService] User Service unavailable"));

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[UserService] User Service unavailable"));
    }

    // ================= GET MY HABITS (GET habit/me) =================

    @Test
    @DisplayName("✅ getMyHabits — 200 OK: returns habits of current user")
    void getMyHabits_Success() throws Exception {
        HabitCreateDto dto1 = new HabitCreateDto();
        dto1.setTitle("Morning Run");
        dto1.setDescription("Run 3 km every morning");
        dto1.setFrequency(Frequency.DAILY);
        dto1.setStartDate(LocalDate.now());
        dto1.setEndDate(LocalDate.now().plusDays(7));
        dto1.setStatus(HabitStatus.ACTIVE);

        HabitCreateDto dto2 = new HabitCreateDto();
        dto2.setTitle("Read Book");
        dto2.setDescription("Read 10 pages before bed");
        dto2.setFrequency(Frequency.DAILY);
        dto2.setStartDate(LocalDate.now());
        dto2.setEndDate(LocalDate.now().plusDays(10));
        dto2.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/habit/me")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Morning Run"))
                .andExpect(jsonPath("$[1].title").value("Read Book"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("❌ getMyHabits — 401 UNAUTHORIZED: no token provided")
    void getMyHabits_Unauthorized() throws Exception {
        mockMvc.perform(get("/habit/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                    .value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ getMyHabits — 404 NOT FOUND: user not found")
    void getMyHabits_UserNotFound() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(null);

        mockMvc.perform(get("/habit/me")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: testUser1"));
    }

    @Test
    @DisplayName("❌ getMyHabits — 502 BAD GATEWAY: User Service unavailable")
    void getMyHabits_UserServiceUnavailable() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1"))
                .thenThrow(new ExternalServiceException("[UserService] User Service unavailable"));

        mockMvc.perform(get("/habit/me")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[UserService] User Service unavailable"));
    }

    // ================= GET HABIT BY ID (GET habit/id) =================

    @Test
    @DisplayName("✅ getHabit — 200 OK: habit found for current user")
    void getHabitById_Success() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Morning Run");
        dto.setDescription("Run 3 km every morning");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(7));
        dto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        String response = mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        HabitDto created = objectMapper.readValue(response, HabitDto.class);

        mockMvc.perform(get("/habit/"
                                + created.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Morning Run"))
                .andExpect(jsonPath("$.description").value("Run 3 km every morning"));
    }

    @Test
    @DisplayName("❌ getHabit — 400 BAD REQUEST: invalid habit ID format")
    void getHabitById_InvalidId() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(get("/habit/invalid-id")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'id'." +
                        " Expected type: Long"));
    }

    @Test
    @DisplayName("❌ getHabit — 401 UNAUTHORIZED: no token provided")
    void getHabitById_Unauthorized() throws Exception {
        mockMvc.perform(get("/habit/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                    .value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ getHabit — 403 FORBIDDEN: habit belongs to another user")
    void getHabitById_Forbidden() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Other User Habit");
        dto.setDescription("Habit of testUser2");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(5));
        dto.setStatus(HabitStatus.ACTIVE);

        String token2 = jwtUtil.generateAccessToken("testUser2");

        Mockito.when(userService.getUserByUsername("testUser2")).thenReturn(testUser2);

        String response = mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        HabitDto created = objectMapper.readValue(response, HabitDto.class);

        mockMvc.perform(get("/habit/"
                                + created.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You don’t have access to this habit"));
    }

    @Test
    @DisplayName("❌ getHabit — 404 NOT FOUND: habit does not exist")
    void getHabitById_NotFound() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/habit/9999")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found with id: 9999"));
    }

    @Test
    @DisplayName("❌ getHabit — 502 BAD GATEWAY: User Service unavailable")
    void getHabitById_UserServiceUnavailable() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1"))
                .thenThrow(new ExternalServiceException("[UserService] User Service unavailable"));

        mockMvc.perform(get("/habit/1")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error")
                        .value("[UserService] User Service unavailable"));
    }

    // ================= UPDATE HABIT(PUT habit/id) =================

    @Test
    @DisplayName("✅ updateHabit — 200 OK: habit updated successfully")
    void updateHabit_Success() throws Exception {
        HabitCreateDto createDto = new HabitCreateDto();
        createDto.setTitle("Morning Run");
        createDto.setDescription("Run 3 km every morning");
        createDto.setFrequency(Frequency.DAILY);
        createDto.setStartDate(LocalDate.now());
        createDto.setEndDate(LocalDate.now().plusDays(7));
        createDto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        String response = mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        HabitDto createdHabit = objectMapper.readValue(response, HabitDto.class);

        HabitUpdateDto updateDto = new HabitUpdateDto();
        updateDto.setTitle("Evening Run");
        updateDto.setDescription("Run 5 km every evening");
        updateDto.setStatus(HabitStatus.ACTIVE);

        mockMvc.perform(put("/habit/" + createdHabit.getId())
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Evening Run"))
                .andExpect(jsonPath("$.description").value("Run 5 km every evening"));
    }

    @Test
    @DisplayName("❌ updateHabit — 400 BAD REQUEST: invalid habit ID format")
    void updateHabit_InvalidId() throws Exception {
        HabitUpdateDto dto = new HabitUpdateDto();
        dto.setTitle("Some title");

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(put("/habit/invalid-id")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("❌ updateHabit — 401 UNAUTHORIZED: missing token")
    void updateHabit_Unauthorized() throws Exception {
        HabitUpdateDto dto = new HabitUpdateDto();
        dto.setTitle("Unauthorized update");

        mockMvc.perform(put("/habit/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                    .value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ updateHabit — 403 FORBIDDEN: habit belongs to another user")
    void updateHabit_Forbidden() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Foreign Habit");
        dto.setDescription("Belongs to user2");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(7));
        dto.setStatus(HabitStatus.ACTIVE);

        String token2 = jwtUtil.generateAccessToken("testUser2");
        Mockito.when(userService.getUserByUsername("testUser2")).thenReturn(testUser2);

        String response = mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        HabitDto created = objectMapper.readValue(response, HabitDto.class);

        HabitUpdateDto updateDto = new HabitUpdateDto();
        updateDto.setTitle("Hacked title");

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(put("/habit/" + created.getId())
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You don’t have access to this habit"));
    }

    @Test
    @DisplayName("❌ updateHabit — 404 NOT FOUND: habit does not exist")
    void updateHabit_NotFound() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        HabitUpdateDto dto = new HabitUpdateDto();
        dto.setTitle("Non-existing Habit");

        mockMvc.perform(put("/habit/9999")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found with id: 9999"));
    }

    @Test
    @DisplayName("❌ updateHabit — 502 BAD GATEWAY: Notification Service unavailable")
    void updateHabit_NotificationServiceUnavailable() throws Exception {
        HabitCreateDto createDto = new HabitCreateDto();
        createDto.setTitle("Morning Run");
        createDto.setDescription("Run 3 km every morning");
        createDto.setFrequency(Frequency.DAILY);
        createDto.setStartDate(LocalDate.now());
        createDto.setEndDate(LocalDate.now().plusDays(7));
        createDto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        String response = mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        HabitDto createdHabit = objectMapper.readValue(response, HabitDto.class);

        HabitUpdateDto dto = new HabitUpdateDto();
        dto.setTitle("Updated Habit");
        dto.setDescription("Updated description");
        dto.setFrequency(Frequency.WEEKLY);
        dto.setStatus(HabitStatus.ACTIVE);

        Mockito.doThrow(new ExternalServiceException("[NotificationClient] Notification Service unavailable"))
                .when(notificationClient).dispatchNotification(any(), any(), any());

        mockMvc.perform(put("/habit/" + createdHabit.getId())
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[NotificationClient] Notification Service" +
                        " unavailable"));
    }

    @Test
    @DisplayName("❌ updateHabit — 502 BAD GATEWAY: User Service unavailable")
    void updateHabit_UserServiceUnavailable() throws Exception {
        HabitUpdateDto dto = new HabitUpdateDto();
        dto.setTitle("Updated Habit");
        dto.setDescription("Updated description");
        dto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1"))
                .thenThrow(new ExternalServiceException("[UserService] User Service unavailable"));

        mockMvc.perform(put("/habit/1")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[UserService] User Service unavailable"));
    }

    // ================= DELETE HABIT (DELETE /habit/{id}) =================

    @Test
    @DisplayName("✅ deleteHabit — 204 NO CONTENT: habit deleted successfully")
    void deleteHabit_Success() throws Exception {
        HabitCreateDto createDto = new HabitCreateDto();
        createDto.setTitle("Habit to delete");
        createDto.setDescription("Will be deleted soon");
        createDto.setFrequency(Frequency.DAILY);
        createDto.setStartDate(LocalDate.now());
        createDto.setEndDate(LocalDate.now().plusDays(5));
        createDto.setStatus(HabitStatus.ACTIVE);

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        String response = mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        HabitDto created = objectMapper.readValue(response, HabitDto.class);

        mockMvc.perform(delete("/habit/" + created.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("❌ deleteHabit — 400 BAD REQUEST: invalid habit ID format")
    void deleteHabit_InvalidId() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(delete("/habit/invalid-id")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("❌ deleteHabit — 401 UNAUTHORIZED: missing token")
    void deleteHabit_Unauthorized() throws Exception {
        mockMvc.perform(delete("/habit/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                    .value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ deleteHabit — 403 FORBIDDEN: trying to delete another user's habit")
    void deleteHabit_Forbidden() throws Exception {
        HabitCreateDto dto = new HabitCreateDto();
        dto.setTitle("Foreign Habit");
        dto.setDescription("Belongs to testUser2");
        dto.setFrequency(Frequency.DAILY);
        dto.setStartDate(LocalDate.now());
        dto.setEndDate(LocalDate.now().plusDays(7));
        dto.setStatus(HabitStatus.ACTIVE);

        String token2 = jwtUtil.generateAccessToken("testUser2");
        Mockito.when(userService.getUserByUsername("testUser2")).thenReturn(testUser2);

        String response = mockMvc.perform(post("/habit")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        HabitDto created = objectMapper.readValue(response, HabitDto.class);

        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(delete("/habit/" + created.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You don’t have access to delete this habit"));
    }

    @Test
    @DisplayName("❌ deleteHabit — 404 NOT FOUND: habit does not exist")
    void deleteHabit_NotFound() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);

        mockMvc.perform(delete("/habit/99999")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found with id: 99999"));
    }

    @Test
    @DisplayName("❌ deleteHabit — 502 BAD GATEWAY: User Service unavailable")
    void deleteHabit_UserServiceUnavailable() throws Exception {
        Mockito.when(userService.getUserByUsername("testUser1"))
                .thenThrow(new ExternalServiceException("[UserService] User Service unavailable"));

        mockMvc.perform(delete("/habit/1")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[UserService] User Service unavailable"));
    }
}