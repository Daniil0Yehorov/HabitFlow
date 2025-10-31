package com.habitFlow.userService.service;

import com.habitFlow.userService.config.NotificationClient;
import com.habitFlow.userService.dto.NotificationSettingsRequest;
import com.habitFlow.userService.dto.UpdateChannelRequest;
import com.habitFlow.userService.dto.UserDto;
import com.habitFlow.userService.exception.custom.ChannelNotSelectedException;
import com.habitFlow.userService.exception.custom.InvalidRequestException;
import com.habitFlow.userService.exception.custom.UserNotFoundException;
import com.habitFlow.userService.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * UserFacade handles business logic related to the currently logged-in user.
 * It abstracts user operations like fetching user info, updating notification channels,
 * and regenerating Telegram tokens, so that controllers remain thin.
 */
@Service
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final NotificationClient notificationClient;

    public ResponseEntity<UserDto> getCurrentUserInfo() {
        String username = getCurrentUsername();
        User user = userService.findByUsername(username);
        if (user == null) throw new UserNotFoundException("User not found");

        return ResponseEntity.ok(new UserDto(user.getId(), user.getUsername(), user.getEmail()));
    }

    public ResponseEntity<String> updateNotificationChannel(UpdateChannelRequest req) {
        if (req.getChannel() == null) {
            throw new InvalidRequestException("Channel is required");
        }

        String username = getCurrentUsername();
        User user = userService.findByUsername(username);
        if (user == null) throw new UserNotFoundException("User not found");

        NotificationSettingsRequest notifReq = buildNotifReq(user);
        try {
            notificationClient.updateNotificationChannel(notifReq, req.getChannel());
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new ChannelNotSelectedException("Telegram channel is not selected");
        } catch (RestClientException ex) {
            throw new RuntimeException("Notification service error: " + ex.getMessage(), ex);
        }

        return ResponseEntity.ok("Notification channel updated to " + req.getChannel());
    }

    public ResponseEntity<String> regenerateTelegramToken() {
        String username = getCurrentUsername();
        User user = userService.findByUsername(username);
        if (user == null) throw new UserNotFoundException("User not found");

        NotificationSettingsRequest req = buildNotifReq(user);

        try {
            notificationClient.regenerateTelegramToken(req);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new ChannelNotSelectedException("Telegram channel is not selected");
        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Notification service error: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            throw new RuntimeException("Notification service error: " + ex.getMessage(), ex);
        }

        return ResponseEntity.ok("A new Telegram token has been sent to your email.");
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private NotificationSettingsRequest buildNotifReq(User user) {
        NotificationSettingsRequest req = new NotificationSettingsRequest();
        req.setUserId(user.getId());
        req.setUsername(user.getUsername());
        req.setEmail(user.getEmail());
        return req;
    }
}