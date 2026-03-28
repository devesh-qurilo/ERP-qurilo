package com.erp.employee_service.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TempJwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public String generate(String email) {

        return Jwts.builder()
                .setSubject(email)
                .claim("temp", true)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 15 * 60 * 1000)
                )
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }
}

