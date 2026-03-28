package com.erp.auth_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class ConfigCheck implements CommandLineRunner {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void run(String... args) throws Exception {
        JavaMailSenderImpl mailSenderImpl = (JavaMailSenderImpl) mailSender;

        System.out.println("=== MAIL CONFIGURATION CHECK ===");
        System.out.println("Mail host: " + mailSenderImpl.getHost());
        System.out.println("Mail port: " + mailSenderImpl.getPort());
        System.out.println("Mail username: " + mailSenderImpl.getUsername());
        System.out.println("Mail protocol: " + mailSenderImpl.getProtocol());

        // Check some properties
        System.out.println("SMTP Auth: " + mailSenderImpl.getJavaMailProperties().getProperty("mail.smtp.auth"));
        System.out.println("STARTTLS Enabled: " + mailSenderImpl.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
        System.out.println("================================");
    }
}