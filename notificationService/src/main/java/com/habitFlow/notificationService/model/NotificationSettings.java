package com.habitFlow.notificationService.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.Builder.Default;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Default
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel = NotificationChannel.EMAIL;;

    private String address;//here will be or email or chatid

    @Default
    private boolean enabled=true;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    private LocalDateTime expiryAt;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
