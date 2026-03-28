package com.erp.chat_service.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

/**
 * AuthChannelInterceptor: validates token on STOMP CONNECT.
 *
 * Important: uses only public StompHeaderAccessor APIs (getFirstNativeHeader / getNativeHeader)
 * to avoid protected-method errors across Spring versions.
 */
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private WebSocketSecurityConfig securityConfig;

    @Autowired
    private Environment env;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Try several header names in a safe, public-API way
            String token = extractTokenFromAccessor(accessor);

            // Optional: also check query param style (some SockJS clients may supply token as "token" native header)
            if (token == null) {
                List<String> maybe = accessor.getNativeHeader("token");
                if (maybe != null && !maybe.isEmpty()) token = maybe.get(0);
            }

            // Optional static-token fallback (commented — you said you'll send token from frontend)
            /*
            if (token == null) {
                String staticToken = env.getProperty("chat.static-token");
                if (staticToken != null && !staticToken.isBlank()) {
                    token = staticToken;
                }
            }
            */

            if (token == null || token.isBlank()) {
                throw new RuntimeException("Authorization token missing for WebSocket connection");
            }

            // Accept "Bearer ..." or raw tokens
            if (token.startsWith("Bearer ")) token = token.substring(7).trim();

            // Validate token via WebSocketSecurityConfig
            if (!securityConfig.validateToken(token)) {
                throw new RuntimeException("Invalid WebSocket token");
            }

            String employeeId = securityConfig.getEmployeeIdFromToken(token);
            accessor.setUser(createPrincipal(employeeId));
        }
        return message;
    }

    /**
     * Tries to extract token using safe, public accessor methods and common header names.
     */
    private String extractTokenFromAccessor(StompHeaderAccessor accessor) {
        // 1) Prefer getFirstNativeHeader when available (convenience)
        try {
            String v = accessor.getFirstNativeHeader("Authorization");
            if (hasText(v)) return v;
        } catch (NoSuchMethodError ignore) {
            // older Spring versions might not have getFirstNativeHeader; safe fallback below
        }

        // 2) Try lowercase header (some clients send 'authorization')
        try {
            String v = accessor.getFirstNativeHeader("authorization");
            if (hasText(v)) return v;
        } catch (NoSuchMethodError ignore) { }

        // 3) Try other common headers (auth, token, x-authorization)
        List<String> list;

        list = accessor.getNativeHeader("Authorization");
        if (list != null && !list.isEmpty()) return list.get(0);

        list = accessor.getNativeHeader("authorization");
        if (list != null && !list.isEmpty()) return list.get(0);

        list = accessor.getNativeHeader("auth");
        if (list != null && !list.isEmpty()) return list.get(0);

        list = accessor.getNativeHeader("token");
        if (list != null && !list.isEmpty()) return list.get(0);

        list = accessor.getNativeHeader("x-authorization");
        if (list != null && !list.isEmpty()) return list.get(0);

        // nothing found
        return null;
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private Principal createPrincipal(String id) {
        Objects.requireNonNull(id, "principal id must not be null");
        return () -> id;
    }
}
