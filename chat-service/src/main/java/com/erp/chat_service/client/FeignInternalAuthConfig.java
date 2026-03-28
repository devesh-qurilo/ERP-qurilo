package com.erp.chat_service.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignInternalAuthConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor forwardAuthAndInternalKey() {
        return template -> {
            // 1) Try to forward Authorization from current HTTP request
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                String auth = req.getHeader("Authorization");
                if (auth != null && !auth.isBlank()) {
                    template.header("Authorization", auth);
                }
            }

            // (Optional) 2) If running @Async where RequestContext missing,
            // try from SecurityContext (if any upstream set one)
            if (!template.headers().containsKey("Authorization")) {
                var authObj = SecurityContextHolder.getContext().getAuthentication();
                if (authObj != null && authObj.getCredentials() instanceof String bearer) {
                    template.header("Authorization", bearer);
                }
            }

            // 3) Always send internal API key
            template.header("X-Internal-Api-Key", internalApiKey);
        };
    }
}
