package com.erp.finance_servic.config;

import com.erp.finance_servic.security.ServiceTokenProvider;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adds "Authorization: Bearer <service-token>" header to all Feign requests.
 * This makes feign calls act as if coming from an admin service (ROLE_ADMIN).
 */
@Component
@RequiredArgsConstructor
public class FeignClientAuthInterceptor implements RequestInterceptor {

    private final ServiceTokenProvider serviceTokenProvider;

    @Override
    public void apply(RequestTemplate template) {
        String token = serviceTokenProvider.getServiceToken();
        if (token != null && !token.isBlank()) {
            template.header("Authorization", "Bearer " + token);
        }
        // Keep X-Internal-Api-Key as well — your existing Feign calls still pass it.
        // No further logic required here; we add Authorization globally.
    }
}
