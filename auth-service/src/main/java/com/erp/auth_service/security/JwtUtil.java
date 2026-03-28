package com.erp.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

@Service
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String employeeId, List<String> roles) {
        return Jwts.builder()
                .setSubject(employeeId)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 days
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String employeeId) {
        Instant now = Instant.now();
        Instant expiryInstant = now.plus(Duration.ofDays(30));
        Date expiryDate = Date.from(expiryInstant);

        log.info("Refresh token JWT expiry: {}", expiryDate); // Debug log

        return Jwts.builder()
                .setSubject(employeeId)
                .setIssuedAt(Date.from(now))
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateToken(String token) {
        try {
            log.info("Validating token: {}", token);
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("Refresh token expired at {}", e.getClaims().getExpiration());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token validation failed");
        }
    }
}