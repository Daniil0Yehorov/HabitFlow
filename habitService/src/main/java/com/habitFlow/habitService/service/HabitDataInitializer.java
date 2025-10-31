package com.habitFlow.habitService.service;

import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.model.HabitTracking;
import com.habitFlow.habitService.model.enums.Frequency;
import com.habitFlow.habitService.model.enums.HabitStatus;
import com.habitFlow.habitService.repository.HabitRepository;
import com.habitFlow.habitService.repository.HabitTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class HabitDataInitializer implements CommandLineRunner {

    private  final HabitRepository habitRepository;
    private final HabitTrackingRepository habitTrackingRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[HabitDataInitializer] — creating random sample habits and trackings...");

        Random random = new Random();

        List<Habit> habits = List.of(
                Habit.builder()
                        .userId((long) (1 + random.nextInt(10)))
                        .title("Morning workout")
                        .description("Do 15 minutes of stretching every morning")
                        .frequency(Frequency.DAILY)
                        .startDate(LocalDate.now().minusDays(3))
                        .endDate(LocalDate.now().plusDays(10))
                        .status(HabitStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),

                Habit.builder()
                        .userId((long) (1 + random.nextInt(10)))
                        .title("Read a book")
                        .description("Read at least 10 pages a day")
                        .frequency(Frequency.DAILY)
                        .startDate(LocalDate.now().minusDays(5))
                        .endDate(LocalDate.now().plusDays(15))
                        .status(HabitStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),

                Habit.builder()
                        .userId((long) (1 + random.nextInt(10)))
                        .title("Drink water")
                        .description("Drink 2 liters of water daily")
                        .frequency(Frequency.DAILY)
                        .startDate(LocalDate.now().minusDays(2))
                        .endDate(LocalDate.now().plusDays(7))
                        .status(HabitStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                Habit.builder()
                        .userId((long) 12640)
                        .title("Drink water")
                        .description("Drink 2 liters of water daily")
                        .frequency(Frequency.DAILY)
                        .startDate(LocalDate.now().minusDays(2))
                        .endDate(LocalDate.now().plusDays(7))
                        .status(HabitStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build(),
                Habit.builder()
                        .userId((long) (12641))
                        .title("Drink water")
                        .description("Drink 2 liters of water daily")
                        .frequency(Frequency.DAILY)
                        .startDate(LocalDate.now().minusDays(2))
                        .endDate(LocalDate.now().plusDays(7))
                        .status(HabitStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()
        );

        List<Habit> savedHabits = habitRepository.saveAll(habits);

        savedHabits.forEach(habit -> {
            for (int i = 0; i < 5; i++) {
                habitTrackingRepository.save(
                        HabitTracking.builder()
                                .habit(habit)
                                .trackDate(LocalDate.now().minusDays(i))
                                .done(i % 2 == 0)
                                .build()
                );
            }
        });

        System.out.println("[HabitDataInitializer] ✅ Created " + savedHabits.size() +
                " habits with 3 trackings each.");}
}