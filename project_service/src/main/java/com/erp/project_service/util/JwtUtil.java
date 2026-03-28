package com.erp.project_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;

import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${auth.jwt-secret}")
    private String jwtSecret;

    private Key signingKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("auth.jwt-secret must be set");
        }
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseTokenClaims(String token) {
        if (token == null) return null;
        String cleaned = token.startsWith("Bearer ") ? token.substring(7) : token;
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(cleaned);
        return jws.getBody();
    }
}
