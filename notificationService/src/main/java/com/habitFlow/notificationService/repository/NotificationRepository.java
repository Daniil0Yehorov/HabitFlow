package com.habitFlow.notificationService.repository;

import com.habitFlow.notificationService.model.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationSettings,Long> {
    Optional<NotificationSettings> findByAddress(String address);
    Optional<NotificationSettings> findByUserIdAndEnabled(Long userId, boolean enabled);

    @Query(value = "SELECT * FROM notification_settings WHERE id > :lastId ORDER BY id ASC LIMIT :limit",
            nativeQuery = true)
    List<NotificationSettings> findTopNByIdGreaterThanOrderByIdAsc(
            @Param("lastId") Long lastId,
            @Param("limit") int limit
    );
}
