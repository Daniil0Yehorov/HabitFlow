package com.habitFlow.notificationService.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceTokenProvider {

    private final JwtUtil jwtUtil;

    public String getServiceToken()
    {
        return jwtUtil.generateServiceToken("notification-service");
    }
}