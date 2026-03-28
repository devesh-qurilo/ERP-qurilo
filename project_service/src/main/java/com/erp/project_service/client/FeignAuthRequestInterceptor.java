package com.erp.project_service.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignAuthRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            HttpServletRequest request = sra.getRequest();
            String auth = request.getHeader("Authorization");
            if (auth != null) {
                template.header("Authorization", auth);
            }
            // Optionally forward other X-* headers like X-Internal-Api-Key if needed
            String internalKey = request.getHeader("X-Internal-Api-Key");
            if (internalKey != null) {
                template.header("X-Internal-Api-Key", internalKey);
            }
        }
    }
}
