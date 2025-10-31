package com.habitFlow.notificationService.service;

import com.habitFlow.notificationService.config.UserService;
import com.habitFlow.notificationService.model.NotificationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CleanUpService {
    private final NotificationService notificationService;
    private final UserService userService;

    private static final int BATCH_SIZE = 10;
    private Long lastProcessedId = 0L;

    //every 30 minutes deletes 10 notification with userid which one doesnt exist
    // in table user from UserService
    @Scheduled(cron = "0 0/30 * * * ?")
    public void cleanupNotification() {
        System.out.println("[CleanUpService] 🔍 Starting cleanup job...");

        List<NotificationSettings> notifications =
                notificationService.findBatchOfNotificationSettings(BATCH_SIZE, lastProcessedId);

        if (notifications.isEmpty()) {
            lastProcessedId = 0L;
            System.out.println("[CleanUpService] Reached end of table, resetting lastProcessedId.");
            return;
        }

        List<Long> toDeleteIds = notifications.stream().filter(setting -> {
                    boolean delete = false;

                    try {
                        boolean userExists = userService.existsById(setting.getUserId());
                        if (!userExists) delete = true;
                    } catch (Exception e) {
                        System.out.println("[CleanUpService] ⚠️ Error checking user " + setting.getUserId()
                                + ": " + e.getMessage());
                    }

                    if (setting.getExpiryAt() != null && setting.getExpiryAt().isBefore(LocalDateTime.now())) {
                        delete = true;
                    }

                    return delete;
                }).map(NotificationSettings::getId)
                .collect(Collectors.toList());

        notificationService.deleteNotificationsById(toDeleteIds);

        lastProcessedId = notifications.get(notifications.size() - 1).getId();
    }
}
