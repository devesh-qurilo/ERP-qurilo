package com.erp.project_service.config;

import com.erp.project_service.security.JwtAuthenticationFilter;
import com.erp.project_service.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtUtil jwtUtil,
                          CorsConfigurationSource corsConfigurationSource) {
        this.jwtUtil = jwtUtil;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtUtil);

        http
                // ✅ CORS ENABLE
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // public endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/public/copy/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // (optional) WebSocket handshake endpoint – agar public rakhna ho
                        .requestMatchers("/ws-discussion/**").permitAll()
                        // baaki sab ke liye auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
