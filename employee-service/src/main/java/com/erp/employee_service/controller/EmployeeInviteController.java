package com.erp.employee_service.controller;

import com.erp.employee_service.controller.email.EmailRequest;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.EmployeeInvite;
import com.erp.employee_service.repository.EmployeeInviteRepository;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.service.MailAsyncService;
import com.erp.employee_service.service.MailService;
import com.erp.employee_service.util.EmployeeIdGenerator;
import com.erp.employee_service.util.TempJwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.token.TokenService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/employee/invite/check")
@RequiredArgsConstructor
@Slf4j
public class EmployeeInviteController {

    private final EmployeeInviteRepository inviteRepo;
    private final EmployeeRepository employeeRepo;
    private final EmployeeIdGenerator idGen;
    private final MailService mailService;
    private final TempJwtUtil tempJwtUtil;
    private final MailAsyncService mailAsyncService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendInvite(@RequestBody EmailRequest req) {

        // 1️⃣ generate employeeId
        String employeeId = idGen.generateTempId();

        // 2️⃣ create minimal employee
        Employee emp = new Employee();
        emp.setEmployeeId(employeeId);
        emp.setEmail(req.to());
        emp.setName("TEMP USER");
        emp.setAbout("INVITED_USER");
        emp.setPassword(UUID.randomUUID().toString());
        emp.setAddress("NA");
        emp.setLanguage("NA");
        emp.setBloodGroup("NA");
        emp.setCountry("NA");
        emp.setGender("NA");
        emp.setBirthday(LocalDate.now());
        emp.setLanguage("NA");
        emp.setLoginAllowed(false);
        emp.setActive(false);
        emp.setJoiningDate(LocalDate.now());
        employeeRepo.save(emp);

        // 3️⃣ create invite token
        String token = UUID.randomUUID().toString();

        EmployeeInvite invite = new EmployeeInvite();
        invite.setEmail(req.to());
        invite.setToken(token);
        invite.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        inviteRepo.save(invite);

        try {
//            mailService.sendInviteEmail(req);
//            mailAsyncService.sendEmployeeInviteAsync(req, token);
        } catch (Exception e) {
            log.error("Mail failed but invite saved", e);
        }

        return ResponseEntity.ok("Invite sent");
    }

    @GetMapping("/accept")
    public ResponseEntity<?> accept(@RequestParam String token) {

        EmployeeInvite invite = inviteRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (invite.getUsed() || invite.isExpired()) {
            throw new RuntimeException("Invite expired");
        }

        String jwt = tempJwtUtil.generate(invite.getEmail());

        invite.setUsed(true);
        inviteRepo.save(invite);

        return ResponseEntity.ok(Map.of("token", jwt));
    }


}

