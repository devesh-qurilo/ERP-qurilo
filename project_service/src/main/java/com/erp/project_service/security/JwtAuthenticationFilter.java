package com.erp.project_service.security;

import com.erp.project_service.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authorization)) {
            try {
                Claims claims = jwtUtil.parseTokenClaims(authorization);
                if (claims != null) {
                    String subject = claims.getSubject(); // employeeId or sub
                    // roles claim might be String or collection
                    Object rolesObj = claims.get("roles");
                    Set<String> roles = new HashSet<>();
                    if (rolesObj instanceof String s) {
                        // could be comma separated or single value
                        if (s.contains(",")) {
                            Arrays.stream(s.split(","))
                                    .map(String::trim).filter(x->!x.isEmpty())
                                    .forEach(roles::add);
                        } else {
                            roles.add(s);
                        }
                    } else if (rolesObj instanceof Collection<?> coll) {
                        coll.forEach(o -> roles.add(String.valueOf(o)));
                    }

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                            .collect(Collectors.toList());

                    // put subject as principal and claims in details if needed
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(subject, null, authorities);
                    auth.setDetails(claims);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ex) {
                // token parsing failed — clear context and continue (unauth access will be rejected downstream)
                SecurityContextHolder.clearContext();
                ex.getMessage();
                // optionally log
                logger.debug("JWT parse failed ");
            }
        }

        filterChain.doFilter(request, response);
    }
}
