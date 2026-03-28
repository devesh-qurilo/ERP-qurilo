package com.erp.finance_servic.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Generates a JWT for service-to-service calls. The token is cached until near expiry.
 * IMPORTANT: Uses same jwt.secret as other services (project-service) so the token validates.
 */
@Component
@RequiredArgsConstructor
public class ServiceTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // cache token to avoid generating each request
    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry;

    public synchronized String getServiceToken() {
        Instant now = Instant.now();
        if (cachedToken == null || cachedTokenExpiry == null || now.isAfter(cachedTokenExpiry.minus(5, ChronoUnit.MINUTES))) {
            // generate new token valid for 30 days (adjust as needed)
            Instant exp = now.plus(30, ChronoUnit.DAYS);
            Key signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            String token = Jwts.builder()
                    .setSubject("finance-service")          // subject = this service
                    .claim("roles", List.of("ROLE_ADMIN"))  // give ADMIN role so project-service accepts
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(exp))
                    .signWith(signingKey)
                    .compact();

            this.cachedToken = token;
            this.cachedTokenExpiry = exp;
        }
        return cachedToken;
    }
}
