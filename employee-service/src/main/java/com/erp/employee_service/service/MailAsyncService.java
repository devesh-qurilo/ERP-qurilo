package com.erp.employee_service.service;

import com.erp.employee_service.controller.email.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailAsyncService {

    private final MailService mailService;

    @Async("taskExecutor")
    public void sendEmployeeInviteAsync(
            EmailRequest req,
            String inviteToken
    ) {
        log.info("Async invite mail started for {}", req.to());
        mailService.sendEmployeeInviteEmail(req, inviteToken);
    }
}

