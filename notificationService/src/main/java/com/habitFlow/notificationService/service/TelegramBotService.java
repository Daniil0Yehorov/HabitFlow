package com.habitFlow.notificationService.service;

import com.habitFlow.notificationService.model.NotificationChannel;
import com.habitFlow.notificationService.model.NotificationSettings;
import com.habitFlow.notificationService.model.NotificationStatus;
import com.habitFlow.notificationService.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramBot telegramBot;
    private final NotificationRepository notificationRepository;

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                try {
                    if (update.message() != null && update.message().text() != null) {
                        String token = update.message().text().trim();
                        Long chatId = update.message().chat().id();

                        Optional<NotificationSettings> chatIdOccupied = notificationRepository.findByAddress(chatId.toString());
                        if (chatIdOccupied.isPresent()) {
                            sendMessage(chatId, "This Telegram account is already linked to another user ❌");
                            continue;
                        }

                        Optional<NotificationSettings> optSettings = notificationRepository.findByAddress(token);
                        if (optSettings.isPresent()) {
                            NotificationSettings settings = optSettings.get();
                            if (settings.getStatus() == NotificationStatus.PENDING
                                    && settings.getExpiryAt().isAfter(LocalDateTime.now())) {
                                settings.setChannel(NotificationChannel.TG);
                                settings.setAddress(chatId.toString());
                                settings.setStatus(NotificationStatus.CONFIRMED);
                                notificationRepository.save(settings);

                                sendMessage(chatId, "Your Telegram account has been successfully linked! ✅");
                            } else {
                                sendMessage(chatId, "Invalid or expired token. ❌");
                            }
                        } else {
                            sendMessage(chatId, "Invalid token. ❌");
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing telegram update: {}", e.getMessage(), e);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public void sendMessage(Long chatId, String text) {
        try {
            telegramBot.execute(new SendMessage(chatId, text));
            log.debug("Telegram sent to {}: {}", chatId, text);
        } catch (Exception e) {
            log.error("Failed to send Telegram to {}: {}", chatId, e.getMessage(), e);
        }
    }
}