package com.erp.gateway_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/auth/") || path.startsWith("/internal/")) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = auth.substring(7).trim();
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String employeeId = claims.getSubject();
            List<String> roles = extractRoles(claims);

            exchange = exchange.mutate().request(
                    exchange.getRequest().mutate()
                            .header("X-Employee-Id", employeeId)
                            .header("X-Roles", String.join(",", roles))
                            .build()
            ).build();

            return chain.filter(exchange);

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private List<String> extractRoles(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream().map(String::valueOf).toList();
        } else if (rolesObj instanceof String) {
            return List.of((String) rolesObj);
        } else if (claims.containsKey("role")) {
            return List.of(String.valueOf(claims.get("role")));
        }
        return List.of();
    }
}
