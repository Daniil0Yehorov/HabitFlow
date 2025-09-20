package com.habitFlow.userService;

import com.habitFlow.userService.model.RefreshToken;
import com.habitFlow.userService.model.User;
import com.habitFlow.userService.repository.RefreshTokenRepository;
import com.habitFlow.userService.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.Instant;

@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}
	/*@Bean
	CommandLineRunner initDatabase(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
		return args -> {
			if (userRepository.count() == 0) {
				User user = new User();
				user.setUsername("admin");
				user.setEmail("Bulbanos@gmail.com");
				user.setPassword("{noop}password");
				userRepository.save(user);

				RefreshToken token = new RefreshToken();
				token.setToken("test-refresh-token");
				token.setExpiryDate(Instant.now().plusSeconds(60 * 60 * 24));
				token.setUser(user);
				refreshTokenRepository.save(token);

				System.out.println("✅ БД инициализирована: создан admin + refresh token");
			}
		};
	}*/
}
