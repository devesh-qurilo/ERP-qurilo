package com.erp.auth_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Arrays;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${auth.admin.emails:}")
    private String adminEmails;

    public MailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;

        // Debug: Check what configuration is being used
        JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;
        System.out.println("=== MAIL SENDER CONFIGURATION ===");
        System.out.println("Host: " + mailSenderImpl.getHost());
        System.out.println("Port: " + mailSenderImpl.getPort());
        System.out.println("Username: " + mailSenderImpl.getUsername());
        System.out.println("Protocol: " + mailSenderImpl.getProtocol());
        System.out.println("================================");
    }

    public void sendOtpMail(String to, String otp) {
        try {
            Context context = new Context();
            context.setVariable("otp", otp);
            String html = templateEngine.process("otp-mail", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("ERP Admin Password Reset OTP");
            helper.setText(html, true);

            mailSender.send(message);
            System.out.println("OTP email sent successfully to: " + to);

        } catch (MessagingException e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    public void sendAdminNotification(String subject, String bodyText) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            // recipients from config
            String[] recipients = adminEmails == null || adminEmails.isBlank()
                    ? new String[]{"admin@example.com"} // fallback
                    : Arrays.stream(adminEmails.split(","))
                    .map(String::trim).toArray(String[]::new);

            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(bodyText, false);
            mailSender.send(message);
            // optional: log.info("Ticket mail sent to admins: {}", Arrays.toString(recipients));
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send admin notification", e);
        }
    }
}