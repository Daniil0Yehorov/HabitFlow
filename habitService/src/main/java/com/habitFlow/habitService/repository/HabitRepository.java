package com.habitFlow.habitService.repository;

import com.habitFlow.habitService.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit,Long> {
    List<Habit> findByUserId(Long userId);
}
