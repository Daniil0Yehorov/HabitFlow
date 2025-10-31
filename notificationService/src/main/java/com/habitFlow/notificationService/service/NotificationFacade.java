package com.habitFlow.notificationService.service;
import com.habitFlow.notificationService.dto.*;
import com.habitFlow.notificationService.model.NotificationSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * NotificationFacade centralizes business logic for notification-related operations.
 * It allows the controller to delegate all processing and focus only on routing & documentation.
 */
@Service
@RequiredArgsConstructor
public class NotificationFacade {
    private final NotificationService notificationService;

    public ResponseEntity<String> sendEmail(EmailRequest request) {
        notificationService.sendEmail(request);
        return ResponseEntity.ok("Email sent successfully!");
    }

    public ResponseEntity<Void> createSettings(NotificationSettingsRequest request) {
        notificationService.createInitialSettings(request);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> updateChannel(UpdateChannelRequest request) {
        NotificationSettings settings = notificationService.getByUserIdAndEnabled(request.getUserId(),
                true);
        notificationService.updateNotificationChannel(
                request.getUserId(),
                request.getChannel(),
                new UserDto(settings.getUserId(), "", settings.getAddress())
        );
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<String> regenerateToken(NotificationSettingsRequest request) {
        notificationService.regenerateTelegramToken(
                request.getUserId(), request.getEmail(), request.getUsername()
        );
        return ResponseEntity.ok("Telegram token regenerated successfully");
    }

    public ResponseEntity<Void> dispatchNotification(DispatchNotificationRequest request) {
        String subject = request.getSubject() != null ? request.getSubject() : "HabitFlow Notification";
        notificationService.notifyUser(request.getUsername(), subject, request.getMessage());
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> confirmEmail(NotificationSettingsRequest request) {
        notificationService.confirmEmailChannel(request.getUserId(), request.getEmail());
        return ResponseEntity.ok().build();
    }
}
