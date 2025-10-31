package com.habitFlow.notificationService.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceJwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        System.out.println("[ServiceJwtFilter] Authorization header: " + request.getHeader("Authorization"));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isServiceToken(token, "HABIT-SERVICE")) {
                System.out.println("[ServiceJwtFilter] Valid token from HABIT-SERVICE");

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "HABIT-SERVICE",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
            else if (jwtUtil.isServiceToken(token, "USER-SERVICE")) {
                System.out.println("[ServiceJwtFilter] Valid token from USER-SERVICE");

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "USER-SERVICE",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
            else {
                System.out.println("[ServiceJwtFilter] Invalid service token");
            }
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("""
        { "error": "Missing ROLE_SERVICE authority" }
    """);
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isServiceToken(token, "HABIT-SERVICE") && !jwtUtil.isServiceToken(token, "USER-SERVICE")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("""
        { "error": "Missing ROLE_SERVICE authority" }
    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/notifications/");
    }
}