package com.erp.client_service.service.notification.impl;

import com.erp.client_service.service.notification.NotificationService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void sendClientWelcome(String toEmail, String clientId, String addedBy) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, "utf-8");
            Context ctx = new Context();
            ctx.setVariable("clientId", clientId);
            ctx.setVariable("addedBy", addedBy);
            String html = templateEngine.process("client-welcome", ctx);
            helper.setText(html, true);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to ERP");
            helper.setFrom("no-reply@example.com");
            mailSender.send(mime);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
