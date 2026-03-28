package com.erp.gateway_service.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class GatewaySecurityFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (path.startsWith("/admin")) {
            if (auth == null || auth.isBlank()) {
                log.warn("Unauthorized access to /admin without token");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            try {
                String token = auth.replaceFirst("Bearer\\s+", "").trim();
                Claims claims = Jwts.parserBuilder().setSigningKey(jwtSecret.getBytes())
                        .build().parseClaimsJws(token).getBody();

                if (!hasAdminRole(claims)) {
                    log.warn("Forbidden: user lacks ROLE_ADMIN");
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

            } catch (Exception e) {
                log.error("JWT parsing failed in GatewaySecurityFilter: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        }

        return chain.filter(exchange);
    }

    private boolean hasAdminRole(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream()
                    .anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(String.valueOf(r)));
        } else if (rolesObj instanceof String) {
            return "ROLE_ADMIN".equalsIgnoreCase((String) rolesObj);
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
