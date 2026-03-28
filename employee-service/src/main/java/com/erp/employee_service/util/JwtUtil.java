package com.erp.employee_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(secret.getBytes()).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token expired");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalid");
        }
    }
}
