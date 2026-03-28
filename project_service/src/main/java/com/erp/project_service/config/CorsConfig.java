package com.erp.project_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ Allow frontend
        config.setAllowedOrigins(List.of(
                "http://localhost:3000"
        ));

        // ✅ Allow required methods
        config.setAllowedMethods(List.of(
                "GET", "POST","PATCH", "PUT", "DELETE", "OPTIONS"
        ));

        // ✅ Allow all headers
        config.setAllowedHeaders(List.of("*"));

        // ✅ Allow Authorization header
        config.setExposedHeaders(List.of("Authorization"));

        // ✅ JWT / cookies support
        config.setAllowCredentials(true);

        // ✅ Cache preflight response
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        // ✅ Apply to ALL endpoints (REST + WebSocket handshake)
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
