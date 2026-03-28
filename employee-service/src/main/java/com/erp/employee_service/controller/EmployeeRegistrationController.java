package com.erp.employee_service.controller;

import com.erp.employee_service.dto.invite.CompleteRegistrationRequest;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.service.impl.EmployeeServiceImpl;
import com.erp.employee_service.util.EmployeeIdGenerator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@RestController
@RequestMapping("/employeeRegister")
@Slf4j
public class EmployeeRegistrationController {

    private final EmployeeRepository employeeRepo;
    private final BCryptPasswordEncoder encoder;
    private final WebClient webClient;
    private final EmployeeIdGenerator idGen;
    @Value("${internal.api.key}")
    private String internalApiKey;

    // ✅ MANUAL CONSTRUCTOR (IMPORTANT)
    public EmployeeRegistrationController(
            EmployeeRepository employeeRepo,
            BCryptPasswordEncoder encoder,
            @Qualifier("authWebClient") WebClient webClient,
            EmployeeIdGenerator idGen
    ) {
        this.employeeRepo = employeeRepo;
        this.encoder = encoder;
        this.webClient = webClient;
        this.idGen = idGen;
    }


    @PostMapping("/complete-registration")
    public ResponseEntity<?> complete(
            Authentication auth,
            @RequestBody CompleteRegistrationRequest req) {

        Claims claims = (Claims) auth.getPrincipal();

        if (!Boolean.TRUE.equals(claims.get("temp", Boolean.class))) {
            throw new RuntimeException("Access denied");
        }

        String email = claims.getSubject();

        Employee emp = employeeRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // 🔥 Generate FINAL employeeId
        String finalEmployeeId = idGen.generateFinalId();

        emp.setEmployeeId(finalEmployeeId); // ✅ overwrite TEMP id
        emp.setName(req.getName());
        emp.setMobile(req.getMobile());
        emp.setGender(req.getGender());
        emp.setBirthday(req.getBirthday());
        emp.setAddress(req.getAddress());
        emp.setPassword(encoder.encode(req.getPassword()));
        emp.setLoginAllowed(true);
        emp.setActive(true);

        employeeRepo.save(emp);

        callAuthRegister(
                emp.getEmployeeId(),
                req.getPassword(),
                emp.getRole(),
                emp.getEmail()
        );

        return ResponseEntity.ok(
                Map.of(
                        "forceLogout", true,
                        "employeeId", finalEmployeeId
                )
        );
    }

    @PatchMapping("/{employeeId}/change-id")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeEmployeeId(
            @PathVariable String employeeId,
            @RequestBody Map<String, String> body) {

        String newId = body.get("newEmployeeId");

        Employee emp = employeeRepo.findByEmployeeId(employeeId)
                .orElseThrow();

        emp.setEmployeeId(newId);
        employeeRepo.save(emp);

        return ResponseEntity.ok("EmployeeId updated");
    }



    private void callAuthRegister(String employeeId, String password, String role, String email) {
        try {
            webClient.post()
                    .uri("/internal/auth/register")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(java.util.Map.of(
                            "employeeId", employeeId,
                            "password", password,
                            "role", role,
                            "email", email))
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                        log.error("Auth register returned {} -> {}", resp.statusCode(), body);
                        return new RuntimeException("Auth register failed: " + resp.statusCode());
                    }))
                    .bodyToMono(Void.class)
                    .block();
            log.info("Auth register succeeded for {}", employeeId);
        } catch (Exception ex) {
            log.error("Auth register failed for {}: {}", employeeId, ex.getMessage());
            throw new RuntimeException("Auth registration failed", ex);
        }
    }
}
