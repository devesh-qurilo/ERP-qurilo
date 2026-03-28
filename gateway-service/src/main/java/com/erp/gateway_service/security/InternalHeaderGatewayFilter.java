package com.erp.gateway_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class InternalHeaderGatewayFilter implements GlobalFilter, Ordered {
    @Value("${internal.api.key:}")
    private String internalApiKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/internal/") && internalApiKey != null && !internalApiKey.isBlank()) {
            exchange.getRequest().mutate().header("X-Internal-Api-Key", internalApiKey).build();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() { return -1; }
}
