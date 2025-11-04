package com.habitFlow.habitService.repository;

import com.habitFlow.habitService.model.HabitTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HabitTrackingRepository extends JpaRepository<HabitTracking,Long> {
    List<HabitTracking> findByHabitId(Long habitId);
    List<HabitTracking> findByHabitIdAndTrackDate(Long habitId, LocalDate trackDate);
    @Query("SELECT ht.habit.id FROM HabitTracking ht WHERE ht.trackDate = :date")
    List<Long> findHabitIdsTrackedOnDate(@Param("date") LocalDate date);
}
