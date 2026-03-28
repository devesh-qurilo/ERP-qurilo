package com.erp.employee_service.service;

import com.erp.employee_service.controller.email.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${mail.from: sagarsatyarthimishra@gmail.com}")
    private String from;

    @Value("${frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    public MailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendWelcomeEmail(String to, String employeeId, String password) {
        try {
            Context ctx = new Context();
            ctx.setVariable("employeeId", employeeId);
            ctx.setVariable("password", password);
            String html = templateEngine.process("welcome-mail", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Welcome to the Company");
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    public void sendProfileUpdateEmail(String to, String changes) {
        try {
            Context ctx = new Context();
            ctx.setVariable("changes", changes);
            String html = templateEngine.process("profile-update-mail", ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("Your profile has been updated");
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send profile update email", e);
        }
    }

    public void sendInviteEmail(EmailRequest req, String employeeId) {
        try {
            Context ctx = new Context();
            // Backend-controlled text (change in template file) + dynamic vars
            ctx.setVariable("customMessage", req.message());      // <-- frontend message goes here
            ctx.setVariable("employeeId", employeeId);   // ⭐ NEW
            ctx.setVariable("loginLink", "https://your-app.example.com/login"); // change as needed
            ctx.setVariable("supportEmail", "support@yourdomain.com"); // change as needed

            // Choose template file name (templates/invite-email.html)
            String html = templateEngine.process("invite-email", ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setTo(req.to());
            helper.setFrom(from);
            helper.setText(html, true); // true = HTML

            mailSender.send(mime);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send invite email: " + e.getMessage(), e);
        }
    }

    // ---------------- NEW METHOD (INVITE FLOW) ----------------

    /**
     * Invite email for onboarding (TEMP LOGIN FLOW)
     */
    public void sendEmployeeInviteEmail(
            EmailRequest req,
            String inviteToken
    ) {
        try {
            Context ctx = new Context();

            String inviteLink = frontendBaseUrl + "/invite?token=" + inviteToken;

            ctx.setVariable("loginLink", inviteLink);
            ctx.setVariable("customMessage", req.message());
            ctx.setVariable("inviterName", "HR Team");
            ctx.setVariable("supportEmail", "support@yourdomain.com");

            String html = templateEngine.process("invite-email", ctx);

            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(req.to());
            helper.setSubject("You're Invited to Join Our Company");
            helper.setText(html, true);

            mailSender.send(mime);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send employee invite email", e);
        }
    }
}
