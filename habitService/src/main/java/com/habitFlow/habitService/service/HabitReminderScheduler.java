package com.habitFlow.habitService.service;

import com.habitFlow.habitService.config.NotificationClient;
import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.dto.UserDto;
import com.habitFlow.habitService.exception.custom.ExternalServiceException;
import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.model.enums.HabitStatus;
import com.habitFlow.habitService.repository.HabitRepository;
import com.habitFlow.habitService.repository.HabitTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class HabitReminderScheduler {

    private final HabitRepository habitRepository;
    private final HabitTrackingRepository habitTrackingRepository;
    private final NotificationClient notificationClient;
    private final UserService userService;

    /**
     * Sends reminders to users every day at 8:00 PM
     * about uncompleted habits for the current day
     */
    @Scheduled(cron = "0 0 20 * * *", zone = "Europe/Berlin")
    public void sendDailyReminders() {
        LocalDate today = LocalDate.now();

        List<Habit> activeHabits = habitRepository.findByStatus(HabitStatus.ACTIVE);

        List<Long> trackedHabitIds = habitTrackingRepository.findHabitIdsTrackedOnDate(today);
        var trackedSet = Set.copyOf(trackedHabitIds);

        List<Long> userIds = activeHabits.stream()
                .map(Habit::getUserId)
                .distinct()
                .toList();

        Map<Long, UserDto> users;
        try {
            users = userService.getUsersByIds(userIds);
        } catch (ExternalServiceException e) {
            System.err.println("[HabitReminderScheduler] 🚨 Failed to fetch users: " + e.getMessage());
            return;
        }

        for (Habit habit : activeHabits) {
            if (trackedSet.contains(habit.getId())) continue;

            UserDto user = users.get(habit.getUserId());
            if (user == null || user.getUsername() == null) {
                System.out.printf("[HabitReminderScheduler] ⚠️ Skipping habit '%s' - no valid user found for id %d%n",
                        habit.getTitle(), habit.getUserId());
                continue;
            }

            String username = user.getUsername();

            try {
                notificationClient.dispatchNotification(
                        username,
                        "Habit Reminder",
                        "Don’t forget to complete your habit '" + habit.getTitle() + "' today! 💪"
                );
                System.out.printf("[HabitReminderScheduler] 🔔 Reminder sent for habit '%s' to '%s'%n",
                        habit.getTitle(), username);
            } catch (Exception e) {
                System.err.printf("[HabitReminderScheduler] ⚠️ Failed to send reminder for habit '%s': %s%n",
                        habit.getTitle(), e.getMessage());
            }
        }
    }

}