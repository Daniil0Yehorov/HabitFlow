package com.habitFlow.habitService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habitFlow.habitService.config.JwtUtil;
import com.habitFlow.habitService.config.NotificationClient;
import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.HabitTrackingDto;
import com.habitFlow.habitService.dto.UserDto;
import com.habitFlow.habitService.exception.custom.ExternalServiceException;
import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.repository.HabitRepository;
import com.habitFlow.habitService.repository.HabitTrackingRepository;
import com.habitFlow.habitService.service.HabitFacade;
import com.habitFlow.habitService.service.HabitService;
import com.habitFlow.habitService.service.HabitTrackingService;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class HabitTrackingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitTrackingRepository habitTrackingRepository;

    @Autowired
    private HabitService habitService;

    @Autowired
    private HabitTrackingService habitTrackingService;

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
        Mockito.when(userService.getUserByUsername("testUser1")).thenReturn(testUser1);
        Mockito.when(userService.getUserByUsername("testUser2")).thenReturn(testUser2);

        habitRepository.deleteAll();
        habitTrackingRepository.deleteAll();
    }

    // ================= CREATE HABIT TRACKING (POST /tracking/) =================

    @Test
    @DisplayName("✅ createTracking — 200 OK: successfully creates tracking record")
    void createTracking_Success() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser1.getId());
        habit.setTitle("Morning Run");
        habit = habitRepository.save(habit);

        HabitTrackingDto dto = HabitTrackingDto.builder()
                .trackDate(LocalDate.of(2025, 10, 21))
                .done(true)
                .build();

        mockMvc.perform(post("/tracking/habit/" + habit.getId())
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.trackDate").value("2025-10-21"));

        Mockito.verify(notificationClient, Mockito.times(1))
                .dispatchNotification(Mockito.eq("testUser1"), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @DisplayName("❌ createTracking — 400 BAD REQUEST: invalid date format")
    void createTracking_InvalidDateFormat() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser1.getId());
        habit.setTitle("Drink Water");
        habit = habitRepository.save(habit);

        HabitTrackingDto badDto = new HabitTrackingDto();
        badDto.setDone(true);

        mockMvc.perform(post("/tracking/habit/" + habit.getId())
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.trackDate").value("Track date must not be null"));
    }

    @Test
    void createTracking_Unauthorized_ShouldReturn401() throws Exception {
        HabitTrackingDto dto = HabitTrackingDto.builder()
                .trackDate(LocalDate.of(2025, 10, 20))
                .done(true)
                .build();

        mockMvc.perform(post("/tracking/habit/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ createTracking — 403 FORBIDDEN: user has no access to this habit")
    void createTracking_Forbidden() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser2.getId());
        habit.setTitle("Read Book");
        habit = habitRepository.save(habit);

        HabitTrackingDto dto = HabitTrackingDto.builder()
                .trackDate(LocalDate.of(2025, 10, 21))
                .done(true)
                .build();

        mockMvc.perform(post("/tracking/habit/" + habit.getId())
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You cannot add tracking for this habit"));
    }

    @Test
    @DisplayName("❌ createTracking — 404 NOT FOUND: habit not found")
    void createTracking_HabitNotFound() throws Exception {
        HabitTrackingDto dto = HabitTrackingDto.builder()
                .trackDate(LocalDate.of(2025, 10, 21))
                .done(false)
                .build();

        mockMvc.perform(post("/tracking/habit/9999")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found with id: 9999"));
    }

    @Test
    @DisplayName("❌ createTracking — 502 BAD GATEWAY: external notification service unavailable")
    void createTracking_ExternalServiceUnavailable() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser1.getId());
        habit.setTitle("Run Every Day");
        habit = habitRepository.save(habit);

        HabitTrackingDto dto = HabitTrackingDto.builder()
                .trackDate(LocalDate.of(2025, 10, 21))
                .done(true)
                .build();

        Mockito.doThrow(new ExternalServiceException("[NotificationClient] Notification Service unavailable"))
                .when(notificationClient)
                .dispatchNotification(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        mockMvc.perform(post("/tracking/habit/" + habit.getId())
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("[NotificationClient] Notification Service unavailable"));

        Mockito.verify(notificationClient, Mockito.times(1))
                .dispatchNotification(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    // ================= GET ALL TRACKINGS FOR HABIT(GET /tracking/habit/{habitId}) =================

    @Test
    @DisplayName("✅ getTrackingsByHabit — 200 OK: successfully returns all tracking records")
    void getTrackingsByHabit_Success() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser1.getId());
        habit.setTitle("Daily Meditation");
        habit = habitRepository.save(habit);

        habitTrackingService.createTracking(
                "testUser1",
                habit.getId(),
                HabitTrackingDto.builder()
                        .trackDate(LocalDate.of(2025, 10, 20))
                        .done(true)
                        .build()
        );
        habitTrackingService.createTracking(
                "testUser1",
                habit.getId(),
                HabitTrackingDto.builder()
                        .trackDate(LocalDate.of(2025, 10, 21))
                        .done(false)
                        .build()
        );

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/" + habit.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].trackDate").value("2025-10-20"))
                .andExpect(jsonPath("$[1].trackDate").value("2025-10-21"));
    }

    @Test
    @DisplayName("❌ getTrackingsByHabit — 401 UNAUTHORIZED: no token provided")
    void getTrackingsByHabit_Unauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ getTrackingsByHabit — 403 FORBIDDEN: user has no access to this habit")
    void getTrackingsByHabit_Forbidden() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser2.getId());
        habit.setTitle("Read Books");
        habit = habitRepository.save(habit);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/" + habit.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You cannot view trackings of this habit"));
    }

    @Test
    @DisplayName("❌ getTrackingsByHabit — 404 NOT FOUND: habit not found")
    void getTrackingsByHabit_HabitNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/9999")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found with id: 9999"));
    }

    @Test
    @DisplayName("❌ getTrackingsByHabit — 400 BAD REQUEST: invalid habit ID format")
    void getTrackingsByHabit_BadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/abc")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'habitId'." +
                        " Expected type: Long"));
    }

    // ================= GET TRACKING BY DATE (GET /tracking/habit/{habitId}/date/{date}) =================

    @Test
    @DisplayName("✅ getTrackingByDate — 200 OK: returns trackings for the given date")
    void getTrackingByDate_Success() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser1.getId());
        habit.setTitle("Morning Routine");
        habit = habitRepository.save(habit);

        habitTrackingService.createTracking(
                "testUser1",
                habit.getId(),
                HabitTrackingDto.builder()
                        .trackDate(LocalDate.of(2025, 10, 21))
                        .done(true)
                        .build()
        );
        habitTrackingService.createTracking(
                "testUser1",
                habit.getId(),
                HabitTrackingDto.builder()
                        .trackDate(LocalDate.of(2025, 10, 20))
                        .done(false)
                        .build()
        );

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/" + habit.getId() + "/date/2025-10-21")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].trackDate").value("2025-10-21"))
                .andExpect(jsonPath("$[0].done").value(true));
    }

    @Test
    @DisplayName("❌ getTrackingByDate — 400 BAD REQUEST: invalid date format")
    void getTrackingByDate_InvalidDateFormat() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser1.getId());
        habit.setTitle("Drink Water");
        habit = habitRepository.save(habit);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/" + habit.getId() + "/date/21-10-2025")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid date format. Use 'YYYY-MM-DD'"));
    }

    @Test
    @DisplayName("❌ getTrackingByDate — 401 UNAUTHORIZED: no token provided")
    void getTrackingByDate_Unauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/1/date/2025-10-21"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ getTrackingByDate — 403 FORBIDDEN: user has no access to this habit")
    void getTrackingByDate_Forbidden() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser2.getId());
        habit.setTitle("Yoga");
        habit = habitRepository.save(habit);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/" + habit.getId() + "/date/2025-10-21")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You cannot view tracking of this habit"));}

    @Test
    @DisplayName("❌ getTrackingByDate — 404 NOT FOUND: habit not found")
    void getTrackingByDate_HabitNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/tracking/habit/9999/date/2025-10-21")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found with id: 9999"));
    }

    // ================= DELETE TRACKING RECORD(DELTE /tracking/{id}) =================

    @Test
    @DisplayName("✅ deleteTracking — 204 NO CONTENT: tracking deleted successfully")
    void deleteTracking_Success() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser1.getId());
        habit.setTitle("Read Book");
        habit = habitRepository.save(habit);

        HabitTrackingDto trackingDto = habitTrackingService.createTracking(
                "testUser1",
                habit.getId(),
                HabitTrackingDto.builder()
                        .trackDate(LocalDate.now())
                        .done(true)
                        .build()
        );

        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/tracking/" + trackingDto.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("❌ deleteTracking — 400 BAD REQUEST: invalid tracking ID parameter (non-numeric)")
    void deleteTracking_InvalidId() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/tracking/invalid-id")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value for parameter 'id'." +
                        " Expected type: Long"));
    }

    @Test
    @DisplayName("❌ deleteTracking — 401 UNAUTHORIZED: no token provided")
    void deleteTracking_Unauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/tracking/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Full authentication is required to access this resource"));
    }

    @Test
    @DisplayName("❌ deleteTracking — 403 FORBIDDEN: user has no access to this tracking")
    void deleteTracking_Forbidden() throws Exception {
        Habit habit = new Habit();
        habit.setUserId(testUser2.getId());
        habit.setTitle("Morning Run");
        habit = habitRepository.save(habit);

        HabitTrackingDto trackingDto = habitTrackingService.createTracking(
                "testUser2",
                habit.getId(),
                HabitTrackingDto.builder()
                        .trackDate(LocalDate.now())
                        .done(false)
                        .build()
        );

        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/tracking/" + trackingDto.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You cannot delete this tracking"));
    }

    @Test
    @DisplayName("❌ deleteTracking — 404 NOT FOUND: tracking not found")
    void deleteTracking_NotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/tracking/9999")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("HabitTracking not found with id: 9999"));
    }
}
