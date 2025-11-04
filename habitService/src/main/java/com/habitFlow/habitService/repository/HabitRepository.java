package com.habitFlow.habitService.repository;

import com.habitFlow.habitService.model.Habit;
import com.habitFlow.habitService.model.enums.HabitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit,Long> {
    List<Habit> findByUserId(Long userId);

    List<Habit> findByStatus(HabitStatus status);

    @Query(value = "SELECT * FROM habit WHERE id > :lastId ORDER BY id ASC LIMIT :limit", nativeQuery = true)
    List<Habit> findTopNByIdGreaterThanOrderByIdAsc(@Param("lastId") Long lastId, @Param("limit") int limit);
}
