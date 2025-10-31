package com.habitFlow.notificationService.service;

import com.habitFlow.notificationService.model.NotificationChannel;
import com.habitFlow.notificationService.model.NotificationSettings;
import com.habitFlow.notificationService.model.NotificationStatus;
import com.habitFlow.notificationService.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class NotificationDataInitializer implements CommandLineRunner {

    private final NotificationRepository notificationRepository;
    private static final int SAMPLE_USERS = 15767;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[NotificationDataInitializer] — creating sample notifications...");

        Random random = new Random();

        for (long userId = 15759; userId <= SAMPLE_USERS; userId++) {
                NotificationSettings settings = NotificationSettings.builder()
                        .userId(userId)
                        .channel(NotificationChannel.EMAIL)
                        .address("user" + userId + "@example.com")
                        .enabled(true)
                        .status(random.nextBoolean() ? NotificationStatus.PENDING : NotificationStatus.CONFIRMED)
                        .createdAt(LocalDateTime.now().minusDays(random.nextInt(10)))
                        .updatedAt(LocalDateTime.now().minusDays(random.nextInt(5)))
                        .expiryAt(random.nextBoolean() ? LocalDateTime.now().plusDays(random.nextInt(5)) : null)
                        .build();

                notificationRepository.save(settings);
        }

        System.out.println("[NotificationDataInitializer] ✅ Sample notifications created successfully.");
    }
}