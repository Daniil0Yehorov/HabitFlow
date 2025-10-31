package com.habitFlow.userService.service;

import com.habitFlow.userService.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CleanupService {

    private final UserService userService;

    //delets users who haven't confirmed their email addresses every hour.
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupUnverifiedUsers() {
        LocalDateTime expiry = LocalDateTime.now().minusHours(24);
        List<User> expiredUsers = userService.findAllByEmailVerifiedFalseAndCreatedAtBefore(expiry);

        if (expiredUsers.isEmpty()) return;

        List<Long> userIds = expiredUsers.stream().map(User::getId).toList();

        userService.deleteAllByIds(userIds);

        System.out.println("[UserCleanupService] Deleted " + userIds.size() + " unverified users");
    }
}