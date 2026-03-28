package com.erp.finance_servic.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:${spring.mail.username}}")
    private String from; // fallback: username

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage mm = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(mm, true, "UTF-8");
            h.setFrom(from);           // 👈 important
            h.setTo(to);
            h.setSubject(subject);
            h.setText(htmlBody, true);
            mailSender.send(mm);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send email: " + ex.getMessage(), ex);
        }
    }
}
