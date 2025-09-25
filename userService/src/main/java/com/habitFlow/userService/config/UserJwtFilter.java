package com.habitFlow.userService.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserJwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //System.out.println("[UserJwtFilter] URI: " + request.getRequestURI());
        //System.out.println("[UserJwtFilter] Before processing: " + SecurityContextHolder.getContext().getAuthentication());
        if (request.getRequestURI().startsWith("/auth/internal/")) {
            //System.out.println("[UserJwtFilter] Skipping internal endpoint");
            filterChain.doFilter(request, response);
            return;
        }
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String username = jwtUtil.extractUsername(token);

                if (!jwtUtil.isTokenValid(token, username)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
                //System.out.println("[UserJwtFilter] Authentication set for user: " + username);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
       // System.out.println("[UserJwtFilter] After filterChain.doFilter: " + SecurityContextHolder.getContext().getAuthentication());
    }
}