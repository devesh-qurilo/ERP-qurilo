package com.erp.chat_service.config;

import com.erp.chat_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

/**
 * Security config with explicit CORS origin (do NOT use "*"
 * when credentials are allowed). Set FRONTEND_ORIGIN in env or
 * application.yml, e.g. FRONTEND_ORIGIN=http://localhost:3000
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Explicit frontend origin. Default to localhost:3000 for dev.
     * IMPORTANT: do not use "*" if your client sends credentials (cookies/withCredentials)
     */
    @Value("${frontend.origin:http://localhost:3000}")
    private String frontendOrigin;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS using our CorsConfigurationSource bean (lambda form)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF for REST API (enable if you need CSRF protection)
                .csrf(csrf -> csrf.disable())
                // stateless session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // authorization rules
                .authorizeHttpRequests(authz -> authz
                        // allow websocket handshake + sockjs info endpoints
                        .requestMatchers("/ws-chat/**", "/ws-chat").permitAll()
                        .requestMatchers("/api/chat/health").permitAll()
                        .anyRequest().authenticated()
                )
                // add your JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(new com.erp.chat_service.config.JwtAuthenticationFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Use explicit allowed origin (do NOT use "*")
        config.setAllowedOrigins(List.of(frontendOrigin));

        // Allowed methods
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));

        // Allow headers commonly sent by browsers / your client
        config.setAllowedHeaders(List.of(
                "Authorization", "Cache-Control", "Content-Type", "X-Requested-With",
                "X-Api-Key", "X-Internal-Api-Key"
        ));

        // Allow credentials (cookies / Authorization header forwarded by browser/STOMP)
        config.setAllowCredentials(true);

        // If you want to expose any headers to browser JS, add them here
        config.setExposedHeaders(List.of("Content-Disposition", "X-Total-Count"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply to all endpoints (including /ws-chat/info used by SockJS)
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
