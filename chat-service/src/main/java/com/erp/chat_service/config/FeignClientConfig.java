package com.erp.chat_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-API-KEY", internalApiKey);
            requestTemplate.header("Content-Type", "application/json");
        };
    }
}