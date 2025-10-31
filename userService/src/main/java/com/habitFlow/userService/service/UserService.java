package com.habitFlow.userService.service;

import com.habitFlow.userService.dto.*;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public UserDto findUserById(String userId) {
        return userRepository.findById(Long.valueOf(userId))
                .map(user -> UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .orElse(null);
    }

    public UserDto findUserDtoByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> UserDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .orElse(null);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<User> findAllByEmailVerifiedFalseAndCreatedAtBefore(LocalDateTime dateTime) {
        return userRepository.findAllByEmailVerifiedFalseAndCreatedAtBefore(dateTime);
    }

    @Transactional
    public void deleteAllByIds(List<Long> ids) {
        userRepository.deleteAllById(ids);
    }

    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }
}
