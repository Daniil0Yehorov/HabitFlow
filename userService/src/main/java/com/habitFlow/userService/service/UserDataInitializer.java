package com.habitFlow.userService.service;

import com.habitFlow.userService.model.User;
import com.habitFlow.userService.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
       for (int i = 1; i <= 30; i++) {
            User user = User.builder()
                    .username("testuser" + i)
                    .password("password")
                    .email("testuser" + i + "@example.com")
                    .emailVerified(false)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();
            userRepository.save(user);
        }
        System.out.println("[UserDataInitializer] Added 3 unverified users");
    }
}