package com.habitFlow.notificationService.exception.custom;

public class NotificationSendException extends RuntimeException {
    public NotificationSendException(String message) {
        super(message);
    }
}