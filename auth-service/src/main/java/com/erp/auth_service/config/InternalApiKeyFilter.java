//    package com.erp.auth_service.config;
//
//    import jakarta.servlet.FilterChain;
//    import jakarta.servlet.ServletException;
//    import jakarta.servlet.http.HttpServletRequest;
//    import jakarta.servlet.http.HttpServletResponse;
//    import org.springframework.beans.factory.annotation.Value;
//    import org.springframework.core.Ordered;
//    import org.springframework.core.annotation.Order;
//    import org.springframework.stereotype.Component;
//    import org.springframework.web.filter.OncePerRequestFilter;
//
//    import java.io.IOException;
//
//    @Component
//    @Order(Ordered.HIGHEST_PRECEDENCE)
//    public class InternalApiKeyFilter extends OncePerRequestFilter {
//
//        @Value("${internal.api.key:}")
//        private String internalApiKey;
//
//        @Override
//        protected boolean shouldNotFilter(HttpServletRequest request) {
//            return !request.getRequestURI().startsWith("/internal/");
//        }
//
//        @Override
//        protected void doFilterInternal(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        FilterChain filterChain) throws ServletException, IOException {
//            String header = request.getHeader("X-Internal-Api-Key");
//            if (header == null || !header.equals(internalApiKey)) {
//                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                response.getWriter().write("{\"error\":\"Missing or invalid internal API key\"}");
//                return;
//            }
//            filterChain.doFilter(request, response);
//        }
//    }
package com.erp.auth_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiKeyFilter.class);

    @Value("${internal.api.key:}")
    private String internalApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean shouldFilter = path.startsWith("/internal/");
        log.debug("InternalApiKeyFilter - Path: {}, Should Filter: {}", path, shouldFilter);
        return !shouldFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("X-Internal-Api-Key");

        log.info("=== INTERNAL API KEY FILTER DEBUG ===");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Received X-Internal-Api-Key header: {}", header);
        log.info("Configured internal.api.key: {}", internalApiKey);
        log.info("Header is null: {}", header == null);
        log.info("Keys match: {}", internalApiKey != null && internalApiKey.equals(header));
        log.info("=====================================");

        if (header == null) {
            log.error("Missing X-Internal-Api-Key header for internal endpoint: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid internal API key\"}");
            return;
        }

        if (!header.equals(internalApiKey)) {
            log.error("Invalid API key received. Expected: '{}' (length: {}), Got: '{}' (length: {})",
                    internalApiKey, internalApiKey != null ? internalApiKey.length() : 0,
                    header, header.length());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid internal API key\"}");
            return;
        }

        log.info("Internal API key validation successful for request: {}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }
}