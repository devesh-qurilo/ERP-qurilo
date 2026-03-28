package com.erp.finance_servic.service.notification;

public interface EmailService {
    void send(String to, String subject, String htmlBody);
}
