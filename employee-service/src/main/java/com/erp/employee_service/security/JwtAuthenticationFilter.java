package com.erp.employee_service.security;

import com.erp.employee_service.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.validateToken(token);
                String subject = claims.getSubject();
                Object rolesObj = claims.get("roles");
                Collection<SimpleGrantedAuthority> authorities = List.of();
                if (rolesObj instanceof List<?>) {
                    authorities = ((List<?>) rolesObj).stream()
                            .map(r -> new SimpleGrantedAuthority(String.valueOf(r))).collect(Collectors.toList());
                } else if (rolesObj instanceof String) {
                    authorities = List.of(new SimpleGrantedAuthority((String) rolesObj));
                }
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(subject, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        chain.doFilter(request, response);
    }
}
