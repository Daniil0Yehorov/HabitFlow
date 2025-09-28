package com.habitFlow.notificationService.controller;

import com.habitFlow.notificationService.dto.EmailRequest;
import com.habitFlow.notificationService.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/email")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {

        notificationService.sendEmail(request);
        return ResponseEntity.ok("Email sent successfully!");
    }
}