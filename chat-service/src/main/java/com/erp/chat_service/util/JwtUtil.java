package com.erp.chat_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractEmployeeId(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                log.error("Token is null or empty");
                return null;
            }

            // Remove Bearer prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Try different possible claim names for employee ID
            String employeeId = extractEmployeeIdFromClaims(claims);

            if (employeeId == null || employeeId.trim().isEmpty()) {
                log.warn("Employee ID not found in token claims. Available claims: {}", claims.keySet());
                return null;
            }

            log.debug("Successfully extracted employeeId: {}", employeeId);
            return employeeId;

        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    private String extractEmployeeIdFromClaims(Claims claims) {
        // Try different possible claim names in order of priority
        if (claims.containsKey("employeeId")) {
            return claims.get("employeeId", String.class);
        }
        if (claims.containsKey("employee_id")) {
            return claims.get("employee_id", String.class);
        }
        if (claims.containsKey("empId")) {
            return claims.get("empId", String.class);
        }
        if (claims.containsKey("sub")) {
            // Sometimes employee ID is in subject
            return claims.getSubject();
        }
        if (claims.containsKey("username")) {
            return claims.get("username", String.class);
        }
        if (claims.containsKey("user_id")) {
            return claims.get("user_id", String.class);
        }

        log.warn("No employee ID found in token claims. Available claims: {}", claims.keySet());
        return null;
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

            // Remove Bearer prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;

        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        throw new RuntimeException("Authorization header is missing or invalid: " + authorizationHeader);
    }

    // New method to debug token contents
    public Claims extractAllClaims(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to extract claims: {}", e.getMessage());
            return null;
        }
    }
}