//package com.erp.project_service.security;
//
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.util.Set;
//import java.util.stream.Collectors;
//
//public final class SecurityUtils {
//    private SecurityUtils() {}
//
//    public static String getCurrentUserId() {
//        Authentication a = SecurityContextHolder.getContext().getAuthentication();
//        return a == null ? null : (String) a.getPrincipal();
//    }
//
//    public static Set<String> getCurrentUserRoles() {
//        Authentication a = SecurityContextHolder.getContext().getAuthentication();
//        if (a == null) return Set.of();
//        return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
//    }
//
//    public static boolean isAdmin() {
//        return getCurrentUserRoles().stream().anyMatch(r -> r.equals("ROLE_ADMIN"));
//    }
//}

package com.erp.project_service.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static String getCurrentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;

        Object principal = a.getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        } else if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal != null) {
            return String.valueOf(principal);
        }
        return null;
    }

    public static Set<String> getCurrentUserRoles() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getAuthorities() == null) return Set.of();
        return a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * Robust admin check:
     * - Case-insensitive
     * - Accepts "ROLE_ADMIN" or "ADMIN" (with or without ROLE_ prefix)
     * - Also resilient to minor casing differences
     */
    public static boolean isAdmin() {
        return getCurrentUserRoles().stream()
                .map(r -> {
                    String rr = r == null ? "" : r.trim();
                    // normalize: drop ROLE_ prefix if present and lower-case
                    if (rr.toUpperCase().startsWith("ROLE_")) {
                        rr = rr.substring(5);
                    }
                    return rr.toLowerCase();
                })
                .anyMatch(normalized -> normalized.equals("admin") || normalized.equals("super_admin"));
    }
}
