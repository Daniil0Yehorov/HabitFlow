package com.habitFlow.userService.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
        //System.out.println("[ServiceJwtFilter] URI: " + request.getRequestURI());
        //System.out.println("[ServiceJwtFilter] Before set Authentication: " + SecurityContextHolder.getContext().getAuthentication());
        //System.out.println("[ServiceJwtFilter] AuthHeader: " + authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
           // System.out.println("[ServiceJwtFilter] Received token: " + token);
            if (jwtUtil.isServiceToken(token)) {
                System.out.println("[ServiceJwtFilter] Service token valid");
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "SERVICE",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                //System.out.println("[ServiceJwtFilter] After set Authentication: " + SecurityContextHolder.getContext().getAuthentication());
            }
            else {
                System.out.println("[ServiceJwtFilter] Invalid service token");
            }
        }

        filterChain.doFilter(request, response);
        //System.out.println("[ServiceJwtFilter] After filterChain.doFilter: " + SecurityContextHolder.getContext().getAuthentication());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/auth/internal/");
    }
}