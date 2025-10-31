package com.habitFlow.habitService.service;

import com.habitFlow.habitService.config.UserService;
import com.habitFlow.habitService.model.Habit;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CleanUpService {
    private final HabitService habitService;
    private final UserService userService;

    private static final int BATCH_SIZE = 5;

    private Long lastProcessedId = 0L;

    // every 30 minutes delete habits by their userid, when it doesnt exist in user table
    @Scheduled(cron = "0 0/30 * * * ?")
    public void cleanupHabits() {
        System.out.println("[HabitCleanup] 🔍 Starting cleanup job...");

        List<Habit> batch = habitService.findBatchOfHabits(lastProcessedId, BATCH_SIZE);

        if (batch.isEmpty()) {
            lastProcessedId = 0L;
            System.out.println("[HabitCleanup] Reached end of table, resetting lastProcessedId.");
            return;
        }

        for (Habit habit : batch) {
            boolean userExists;
            try {
                userExists = userService.existsById(habit.getUserId());
            } catch (Exception e) {
                System.out.println("[HabitCleanup] ⚠️ Failed to check user " + habit.getUserId() + ": "
                        + e.getMessage());
                continue;
            }

            if (!userExists) {
                habitService.deleteHabitByIdWithTrackings(habit.getId());
                System.out.println("[HabitCleanup] 🧹 Deleted habit " + habit.getId() + " for non-existent" +
                        " user " + habit.getUserId());
            }
        }

        lastProcessedId = batch.get(batch.size() - 1).getId();
    }
}
