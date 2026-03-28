package com.erp.client_service.service.notification;

public interface NotificationService {
    void sendClientWelcome(String email, String clientId, String addedBy);
}
