package com.erp.client_service.security;

import com.erp.client_service.util.JwtUtil;
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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String subject = jwtUtil.extractSubject(token);
                    List<String> roles = jwtUtil.extractRoles(token);
                    List<SimpleGrantedAuthority> auth = roles.stream()
                            .map(r -> new SimpleGrantedAuthority(r))
                            .collect(Collectors.toList());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(subject, null, auth);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // invalid token -> continue without authentication (controller's @PreAuthorize will reject)
                ex.printStackTrace();
            }
        }
        filterChain.doFilter(request, response);
    }
}
