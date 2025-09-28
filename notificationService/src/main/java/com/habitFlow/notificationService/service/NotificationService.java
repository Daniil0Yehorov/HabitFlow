package com.habitFlow.notificationService.service;

import com.habitFlow.notificationService.dto.EmailRequest;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

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
            throw new RuntimeException("[NotificationService] Failed to send email", e);
        }

    }
}