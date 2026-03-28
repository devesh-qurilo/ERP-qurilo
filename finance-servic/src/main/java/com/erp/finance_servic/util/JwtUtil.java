package com.erp.finance_servic.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private Key getSigningKey() {
        // ensure UTF-8 to avoid platform charset issues
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    public String extractSubject(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public List<String> extractRoles(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        Object roles = claims.get("roles");
        if (roles instanceof List<?> lst) {
            List<String> out = new ArrayList<>();
            lst.forEach(o -> out.add(String.valueOf(o)));
            return out;
        } else if (roles instanceof String str) {
            return Arrays.asList(str.split(","));
        }
        return List.of();
    }
}
