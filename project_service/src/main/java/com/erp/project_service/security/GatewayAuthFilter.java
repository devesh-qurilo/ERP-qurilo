package com.erp.project_service.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads headers injected by gateway:
 * - X-Employee-Id -> principal (string)
 * - X-Roles -> comma separated roles (e.g. ROLE_ADMIN,ROLE_EMPLOYEE)
 *
 * Sets SecurityContext accordingly so controllers/services can call SecurityUtils.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GatewayAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_EMPLOYEE = "X-Employee-Id";
    public static final String HEADER_ROLES = "X-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String employeeId = request.getHeader(HEADER_EMPLOYEE);
            String roles = request.getHeader(HEADER_ROLES);

            if (employeeId != null && !employeeId.isBlank()) {
                List<SimpleGrantedAuthority> authorities = (roles == null || roles.isBlank()) ? List.of() :
                        Arrays.stream(roles.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(employeeId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ex) {
            // don't fail the request here; let downstream security handle it
            ex.getMessage();
            logger.warn("GatewayAuthFilter failed to set auth: {}");
        }

        filterChain.doFilter(request, response);
    }
}
