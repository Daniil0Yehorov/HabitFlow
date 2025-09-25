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

}
