package com.habitFlow.habitService.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceTokenProvider {

    private final JwtUtil jwtUtil;

    public String getServiceToken()
    {
        return jwtUtil.generateServiceToken("habit-service");
    }
}