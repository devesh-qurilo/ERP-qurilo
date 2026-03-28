package com.erp.client_service.event.listeners;

import com.erp.client_service.event.events.ClientCreatedEvent;

import com.erp.client_service.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;

    @EventListener
    public void handleClientCreated(ClientCreatedEvent event) {
        if (event.getEmail() != null && !event.getEmail().isBlank()) {
            notificationService.sendClientWelcome(event.getEmail(), event.getClientId(), event.getAddedBy());
        }
    }
}
