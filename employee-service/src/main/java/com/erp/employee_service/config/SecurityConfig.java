package com.erp.employee_service.config;

import com.erp.employee_service.security.JwtAuthenticationFilter;
import com.erp.employee_service.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil);

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ---- PUBLIC first (ORDER MATTERS) ----
                        .requestMatchers("/employee/meta/**", "/employee/exists/**").permitAll()
                        .requestMatchers("/employee/notifications/internal/**").permitAll()

                        // ---- Authenticated (role rules via @PreAuthorize in controllers) ----
                        .requestMatchers("/employee/me").authenticated()
                        .requestMatchers("/employee/**").authenticated()
                        .requestMatchers("/notifications/**").authenticated()

                        // बाकी सब allow (जरूरत हो तो tighten कर सकते हैं)
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401/403 proper responses (ताकि 500 की जगह सही code मिले)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Access Denied\"}");
                        })
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
