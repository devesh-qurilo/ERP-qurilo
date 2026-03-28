package com.erp.lead_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret is not configured (jwt.secret)");
        }
        // use HMAC-SHA key
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extract the 'sub' (subject) claim from a JWT token string (raw token, not "Bearer ...").
     */
    public String extractSubject(String token) {
        if (token == null) return null;
        try {
            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return parsed.getBody().getSubject();
        } catch (Exception e) {
            log.warn("Failed to parse JWT subject: {}", e.getMessage());
            return null;
        }
    }



    /**
     * Returns true if token has an admin role. This method tries to be tolerant of multiple claim shapes:
     * - roles: ["ROLE_ADMIN", ...]
     * - role: "ROLE_ADMIN"
     * - authorities: ["ADMIN", ...] or ["ROLE_ADMIN", ...]
     * - scope / scopes (space or comma separated)
     */
    public boolean isAdmin(String token) {
        if (token == null) return false;
        try {
            Jws<Claims> parsed = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = parsed.getBody();

            // 1) Check 'roles' claim (common)
            Object rolesObj = claims.get("roles");
            if (rolesObj != null) {
                if (rolesObj instanceof Collection) {
                    Collection<?> col = (Collection<?>) rolesObj;
                    for (Object o : col) {
                        if (isAdminString(String.valueOf(o))) return true;
                    }
                } else if (rolesObj instanceof String) {
                    // maybe comma separated or single
                    String s = (String) rolesObj;
                    for (String part : splitToParts(s)) if (isAdminString(part)) return true;
                }
            }

            // 2) Check 'authorities'
            Object authObj = claims.get("authorities");
            if (authObj != null) {
                if (authObj instanceof Collection) {
                    for (Object o : (Collection<?>) authObj) {
                        if (isAdminString(String.valueOf(o))) return true;
                    }
                } else if (authObj instanceof String) {
                    for (String part : splitToParts((String) authObj)) if (isAdminString(part)) return true;
                }
            }

            // 3) Check single 'role' claim
            Object roleObj = claims.get("role");
            if (roleObj != null && isAdminString(String.valueOf(roleObj))) return true;

            // 4) Check 'scope' or 'scopes' claims (space/comma separated)
            Object scopeObj = claims.get("scope");
            if (scopeObj != null) {
                for (String part : splitToParts(String.valueOf(scopeObj))) if (isAdminString(part)) return true;
            }
            Object scopesObj = claims.get("scopes");
            if (scopesObj != null) {
                for (String part : splitToParts(String.valueOf(scopesObj))) if (isAdminString(part)) return true;
            }

            // 5) fallback: check 'authorities' inside a nested map (some tokens carry details)
            // e.g., claim "realm_access": {"roles": ["ROLE_ADMIN"]}
            Object realmAccess = claims.get("realm_access");
            if (realmAccess instanceof Map) {
                Object rroles = ((Map<?, ?>) realmAccess).get("roles");
                if (rroles instanceof Collection) {
                    for (Object o : (Collection<?>) rroles) if (isAdminString(String.valueOf(o))) return true;
                } else if (rroles instanceof String) {
                    for (String part : splitToParts((String) rroles)) if (isAdminString(part)) return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.warn("Failed to parse JWT for admin check: {}", e.getMessage());
            return false;
        }
    }

    private boolean isAdminString(String s) {
        if (s == null) return false;
        String cleaned = s.trim();
        // Common admin markers
        return cleaned.equalsIgnoreCase("ROLE_ADMIN") ||
                cleaned.equalsIgnoreCase("ADMIN") ||
                cleaned.equalsIgnoreCase("ROLE_SUPERADMIN") ||
                cleaned.equalsIgnoreCase("SUPERADMIN");
    }

    private List<String> splitToParts(String s) {
        if (s == null) return Collections.emptyList();
        if (s.contains(",")) {
            String[] parts = s.split(",");
            List<String> out = new ArrayList<>();
            for (String p : parts) if (!p.isBlank()) out.add(p.trim());
            return out;
        } else {
            // space separated?
            String[] parts = s.split("\\s+");
            List<String> out = new ArrayList<>();
            for (String p : parts) if (!p.isBlank()) out.add(p.trim());
            return out;
        }
    }
}
