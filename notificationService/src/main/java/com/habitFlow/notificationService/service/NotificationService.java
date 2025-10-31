package com.habitFlow.notificationService.service;

import com.habitFlow.notificationService.config.UserService;
import com.habitFlow.notificationService.dto.EmailRequest;
import com.habitFlow.notificationService.dto.NotificationSettingsRequest;
import com.habitFlow.notificationService.dto.UserDto;
import com.habitFlow.notificationService.exception.custom.ForbiddenActionException;
import com.habitFlow.notificationService.exception.custom.NotificationNotFoundException;
import com.habitFlow.notificationService.exception.custom.NotificationSendException;
import com.habitFlow.notificationService.model.NotificationChannel;
import com.habitFlow.notificationService.model.NotificationSettings;
import com.habitFlow.notificationService.model.NotificationStatus;
import com.habitFlow.notificationService.repository.NotificationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository settingsRepo;
    private final TelegramBotService telegramBotService;
    private final UserService userService;

    @Value("${spring.mail.username}")
    private String username;

    public void sendEmail(EmailRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(request.getTo());
            helper.setFrom(username);
            helper.setSubject(request.getSubject());
            helper.setText(request.getMessage(), false);

            mailSender.send(message);
            System.out.println("[NotificationService] Email sent to " + request.getTo());
        } catch (Exception e) {
            NotificationSettings settings = settingsRepo.findByAddress(request.getTo()).orElse(null);
            if (settings != null) {
                settings.setStatus(NotificationStatus.FAILED);
                settings.setUpdatedAt(LocalDateTime.now());
                settingsRepo.save(settings);
            }
            throw new NotificationSendException("Failed to send email to " + request.getTo());
        }

    }

   public void createInitialSettings(NotificationSettingsRequest request) {
       NotificationSettings settings = NotificationSettings.builder()
               .userId(request.getUserId())
               .channel(NotificationChannel.EMAIL)
               .address(request.getEmail())
               .enabled(true)
               .status(NotificationStatus.PENDING)
               .createdAt(LocalDateTime.now())
               .updatedAt(LocalDateTime.now())
               .build();

       settingsRepo.save(settings);
   }

    public void updateNotificationChannel(Long userId, NotificationChannel newChannel, UserDto userDto) {
        NotificationSettings settings = settingsRepo.findByUserIdAndEnabled(userId, true)
                .orElseThrow(() -> new NotificationNotFoundException("Notification settings not found"));

        if (settings.getChannel() != newChannel) {
            settings.setChannel(newChannel);

            switch (newChannel) {
                case TG -> {
                    settings.setAddress(null);
                    settings.setStatus(NotificationStatus.PENDING);
                }
                case EMAIL -> {
                    settings.setAddress(userDto.getEmail());
                    settings.setStatus(NotificationStatus.CONFIRMED);
                }
                case NONE -> {
                    settings.setAddress(null);
                    settings.setStatus(NotificationStatus.DISABLED);
                }
            }
            settings.setUpdatedAt(LocalDateTime.now());
            settingsRepo.save(settings);
        }
    }

    public void regenerateTelegramToken(Long userId, String email, String username) {
        NotificationSettings settings = settingsRepo.findByUserIdAndEnabled(userId, true)
                .orElseThrow(() -> new NotificationNotFoundException("Notification settings not found"));

        if (settings.getChannel() != NotificationChannel.TG) {
            settings.setStatus(NotificationStatus.FAILED);
            settingsRepo.save(settings);
            throw new ForbiddenActionException("Telegram channel is not selected");
        }

        String tgToken = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        settings.setAddress(tgToken);
        settings.setStatus(NotificationStatus.PENDING);
        settings.setExpiryAt(LocalDateTime.now().plusHours(24));
        settings.setUpdatedAt(LocalDateTime.now());
        settingsRepo.save(settings);

        EmailRequest emailRequest = new EmailRequest();
        emailRequest.setTo(email);
        emailRequest.setSubject("Your Telegram token for HabitFlow");
        emailRequest.setMessage(
                "Hi " + username + "!\n\n" +
                        "Here is your Telegram token. It is valid for 24 hours:\n\n" +
                        tgToken + "\n\n" +
                        "Start the bot and enter this token."
        );
        sendEmail(emailRequest);
    }

    public void notifyUser(String username, String subject, String message) {
        UserDto userDto = userService.getUserByUsername(username);

        NotificationSettings settings = settingsRepo.findByUserIdAndEnabled(userDto.getId(), true)
                .orElseThrow(() -> new NotificationNotFoundException("No notification settings for user " + username));

        switch (settings.getChannel()) {
            case EMAIL -> {
                EmailRequest email = new EmailRequest();
                email.setTo(userDto.getEmail());
                email.setSubject(subject);
                email.setMessage(message);
                sendEmail(email);
            }
            case TG -> {
                if (settings.getStatus() == NotificationStatus.CONFIRMED) {
                    Long chatId = Long.valueOf(settings.getAddress());
                    telegramBotService.sendMessage(chatId, message);
                } else {
                    settings.setStatus(NotificationStatus.FAILED);
                    settingsRepo.save(settings);
                    throw new ForbiddenActionException("Telegram channel not confirmed for user " + username);
                }
            }
            case NONE -> throw new ForbiddenActionException("Notifications disabled for user " + username);
        }
    }

    public void confirmEmailChannel(Long userId, String email) {
        NotificationSettings settings = settingsRepo.findByUserIdAndEnabled(userId, true)
                .orElseThrow(() -> new NotificationNotFoundException("Notification settings not found"));

        settings.setChannel(NotificationChannel.EMAIL);
        settings.setAddress(email);
        settings.setStatus(NotificationStatus.CONFIRMED);
        settings.setUpdatedAt(LocalDateTime.now());
        settingsRepo.save(settings);
    }

    public List<NotificationSettings> findBatchOfNotificationSettings(int limit, Long lastId) {
        return settingsRepo.findTopNByIdGreaterThanOrderByIdAsc(lastId, limit);
    }

    @Transactional
    public void deleteNotificationsById(List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return;
        settingsRepo.deleteAllByIdInBatch(notificationIds);
        System.out.println("[NotificationService] 🧹 Deleted " + notificationIds.size()
                + " notifications by ID batch.");
    }

    public NotificationSettings getByUserIdAndEnabled(Long userId, boolean enabled) {
        return settingsRepo.findByUserIdAndEnabled(userId, enabled)
                .orElseThrow(() -> new NotificationNotFoundException("Notification settings not found"));
    }

}