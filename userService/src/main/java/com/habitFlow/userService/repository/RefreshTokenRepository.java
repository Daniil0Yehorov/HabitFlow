package com.habitFlow.userService.repository;

import com.habitFlow.userService.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {

    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(Long userId);

}
