package com.erp.employee_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(@Value("${spring.mail.username}") String username,
                                         @Value("${spring.mail.password}") String password,
                                         @Value("${spring.mail.host:smtp.gmail.com}") String host,
                                         @Value("${spring.mail.port:587}") int port) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");       // MUST for Gmail on 587
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", host);               // trust host (smtp.gmail.com)
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "true");

        return mailSender;
    }
}
