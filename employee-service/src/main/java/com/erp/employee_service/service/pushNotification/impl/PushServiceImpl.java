package com.erp.employee_service.service.pushNotification.impl;

import com.erp.employee_service.entity.pushNotification.PushToken;
import com.erp.employee_service.repository.PushTokenRepository;
import com.erp.employee_service.service.pushNotification.PushService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PushServiceImpl implements PushService {

    private final PushTokenRepository tokenRepo;
    private final WebClient webClient;

    @Value("${push.expo.api:https://exp.host/--/api/v2/push/send}")
    private String expoApiUrl;

    // Explicit constructor with Qualifier to avoid ambiguity
    public PushServiceImpl(PushTokenRepository tokenRepo,
                           @Qualifier("pushWebClient") WebClient webClient) {
        this.tokenRepo = tokenRepo;
        this.webClient = webClient;
    }

    @Override
    @Transactional
    public void registerToken(String employeeId, String provider, String token, String deviceInfo) {
        Optional<PushToken> existing = tokenRepo.findByToken(token);
        PushToken pt;
        if (existing.isPresent()) {
            pt = existing.get();
            pt.setEmployeeId(employeeId);
            pt.setProvider(provider);
            pt.setDeviceInfo(deviceInfo);
            pt.setLastSeen(LocalDateTime.now());
        } else {
            pt = new PushToken();
            pt.setEmployeeId(employeeId);
            pt.setProvider(provider);
            pt.setToken(token);
            pt.setDeviceInfo(deviceInfo);
            pt.setLastSeen(LocalDateTime.now());
        }
        tokenRepo.save(pt);
        log.info("Registered push token for {} provider={} token={}", employeeId, provider, token);
    }

    @Override
    @Transactional
    public void unregisterToken(String token) {
        tokenRepo.deleteByToken(token);
        log.info("Unregistered push token {}", token);
    }

    @Override
    public void sendPushToUser(String employeeId, String title, String body, Map<String, String> data) {
        sendPushToUsers(List.of(employeeId), title, body, data);
    }

    @Override
    @Async
    public void sendPushToUsers(List<String> employeeIds, String title, String body, Map<String, String> data) {
        // fetch tokens
        List<PushToken> tokens = employeeIds.stream()
                .flatMap(eid -> tokenRepo.findByEmployeeId(eid).stream())
                .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            log.debug("No push tokens for recipients {}", employeeIds);
            return;
        }

        // Group by provider for different send strategies (EXPO / FCM)
        Map<String, List<PushToken>> byProvider = tokens.stream().collect(Collectors.groupingBy(PushToken::getProvider));

        // Send EXPO
        List<PushToken> expoTokens = byProvider.getOrDefault("EXPO", List.of());
        if (!expoTokens.isEmpty()) {
            sendExpoPush(expoTokens, title, body, data);
        }

        // TODO: Add FCM sending later if needed (requires Firebase Admin SDK)
    }

    private void sendExpoPush(List<PushToken> expoTokens, String title, String body, Map<String, String> data) {
        // build messages
        List<Map<String, Object>> messages = expoTokens.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("to", t.getToken());
            m.put("title", title);
            m.put("body", body);
            m.put("sound", "default");
            if (data != null) m.put("data", data);
            return m;
        }).collect(Collectors.toList());

        // chunking (Expo recommends <=100 messages per request)
        int chunkSize = 100;
        for (int i = 0; i < messages.size(); i += chunkSize) {
            List<Map<String, Object>> chunk = messages.subList(i, Math.min(messages.size(), i + chunkSize));
            try {
                // Using WebClient to POST to Expo
                webClient.post()
                        .uri(expoApiUrl)
                        .bodyValue(chunk)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnNext(s -> log.debug("Expo response chunk: {}", s))
                        .doOnError(e -> log.error("Expo push error: {}", e.getMessage()))
                        .onErrorResume(e -> Mono.empty())
                        .block(); // block inside @Async is OK; or use reactive pipeline
            } catch (Exception ex) {
                log.error("Failed to send expo chunk: {}", ex.getMessage(), ex);
            }
        }
    }
}
