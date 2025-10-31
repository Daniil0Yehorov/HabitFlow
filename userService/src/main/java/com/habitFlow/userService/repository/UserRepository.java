package com.habitFlow.userService.repository;

import com.habitFlow.userService.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAllByEmailVerifiedFalseAndCreatedAtBefore(LocalDateTime dateTime);
}
