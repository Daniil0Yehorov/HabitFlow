package com.habitFlow.habitService.repository;

import com.habitFlow.habitService.model.HabitTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HabitTrackingRepository extends JpaRepository<HabitTracking,Long> {
    List<HabitTracking> findByHabitId(Long habitId);
    List<HabitTracking> findByHabitIdAndTrackDate(Long habitId, LocalDate trackDate);
}
