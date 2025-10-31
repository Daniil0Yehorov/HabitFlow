package com.habitFlow.habitService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DispatchNotificationRequest {
    private String username;
    private String subject;
    private String message;
}