package com.erp.chat_service.config;

import com.erp.chat_service.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Validates Bearer JWT on REST requests and sets SecurityContext.
 * No static-token logic — expects valid JWT from auth-service.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip WebSocket handshake endpoint - WebSocket auth handled separately by AuthChannelInterceptor
        if (path.startsWith("/ws-chat")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip health check endpoint
        if (path.equals("/api/chat/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7).trim();
                if (jwtUtil.validateToken(token)) {
                    String employeeId = jwtUtil.extractEmployeeId(token); // returns String

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    employeeId,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                logger.warn("JWT token validation failed: " + e.getMessage());
                // Continue filter chain without authentication
            }
        }

        filterChain.doFilter(request, response);
    }
}
