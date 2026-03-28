package com.erp.employee_service.controller;

import com.erp.employee_service.controller.email.EmailRequest;
import com.erp.employee_service.dto.CreateEmployeeRequest;
import com.erp.employee_service.dto.EmployeeResponse;
import com.erp.employee_service.dto.UpdateEmployeeRequest;
import com.erp.employee_service.dto.meta.EmployeeMetaDto;
import com.erp.employee_service.dto.settings.Profile;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.service.EmployeeService;
import com.erp.employee_service.service.MailService;
import com.erp.employee_service.util.EmployeeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employee")
@Slf4j
public class EmployeeController {

    private final EmployeeService svc;
    private final EmployeeIdGenerator idGen;
    private final EmployeeRepository employeeRepo;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder encoder;
    private final MailService mailService;
    private final EmployeeRepository employeeRepository;
    private final WebClient webClient;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public EmployeeController(EmployeeService svc,
                              ObjectMapper objectMapper,
                              MailService mailService,
                              EmployeeIdGenerator idGen,
                              EmployeeRepository employeeRepo,
                              BCryptPasswordEncoder encoder,
                              @Qualifier("authWebClient") WebClient webClient,
                              EmployeeRepository employeeRepository) {
        this.svc = svc;
        this.objectMapper = objectMapper;
        this.mailService = mailService;
        this.employeeRepository = employeeRepository;
        this.idGen = idGen;
        this.employeeRepo = employeeRepository;
        this.encoder = encoder;
        this.webClient = webClient;
    }

    // Admin creates employee with optional profile picture in same request
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<EmployeeResponse> create(
            @RequestPart("employee") String employeeJson,
            @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {

        // Add debug logging
        log.info("Received create request with file: {}", file != null ? file.getOriginalFilename() : "null");
        log.info("Employee JSON: {}", employeeJson);

        // parse JSON string to DTO with clear error handling
        CreateEmployeeRequest req;
        try {
            req = objectMapper.readValue(employeeJson, CreateEmployeeRequest.class);
        } catch (Exception ex) {
            log.error("Failed to parse employee JSON: {}", employeeJson, ex);
            throw new IllegalArgumentException("Invalid 'employee' JSON part: " + ex.getMessage(), ex);
        }

        EmployeeResponse resp = svc.createEmployeeWithProfile(req, file);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(svc.getAll(pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @GetMapping("/all")
    public ResponseEntity<List<EmployeeResponse>> lists() {
        return ResponseEntity.ok(svc.getAllEmployee());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> get(@PathVariable String employeeId) {
        return ResponseEntity.ok(svc.getByEmployeeId(employeeId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> me(Authentication auth) {
        String employeeId = auth.getName();
        return ResponseEntity.ok(svc.getByEmployeeIdOrThrow(employeeId));
    }

    // Allow ADMIN to update employee and optionally upload a new profile picture in same request
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{employeeId}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<EmployeeResponse> updateWithProfile(
            @PathVariable String employeeId,
            @RequestPart("employee") String employeeJson,
            @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {

        UpdateEmployeeRequest req;
        try {
            req = objectMapper.readValue(employeeJson, UpdateEmployeeRequest.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid 'employee' JSON part: " + ex.getMessage(), ex);
        }

        EmployeeResponse resp = svc.updateByEmployeeIdWithProfile(employeeId, req, file);
        return ResponseEntity.ok(resp);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable String employeeId, @Valid @RequestBody UpdateEmployeeRequest req) {
        return ResponseEntity.ok(svc.updateByEmployeeId(employeeId, req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{employeeId}/role")
    public ResponseEntity<EmployeeResponse> updateRole(@PathVariable String employeeId, @RequestBody java.util.Map<String,String> body) {
        String role = body.get("role");
        return ResponseEntity.ok(svc.updateRoleByEmployeeId(employeeId, role));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{employeeId}")
    public ResponseEntity<?> delete(@PathVariable String employeeId) {
        svc.deleteByEmployeeId(employeeId);
        return ResponseEntity.ok(java.util.Map.of("status","success"));
    }

    //Profile Settings

    @PreAuthorize("isAuthenticated()")
    @PutMapping(value = "/me", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<EmployeeResponse> profileSettings(
            Authentication auth,
            @RequestPart("employee") String employeeJson,
            @RequestPart(value = "file", required = false) MultipartFile file) throws Exception {
        String employeeId = auth.getName();
        Profile req;
        try {
            req = objectMapper.readValue(employeeJson, Profile.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid 'employee' JSON part: " + ex.getMessage(), ex);
        }

        EmployeeResponse resp = svc.updateProfileByEmployeeIdSettings(employeeId, req, file);
        return ResponseEntity.ok(resp);
    }

    //Invite Employee By mail
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/invite")
    public ResponseEntity<?> invite(@Valid @RequestBody EmailRequest request) {
        // 1️⃣ generate employeeId
        String employeeId = idGen.generateTempId();
        String password = "password123";

        // 2️⃣ create minimal employee
        Employee emp = new Employee();
        emp.setEmployeeId(employeeId);
        emp.setEmail(request.to());
        emp.setName("TEMP USER");
        emp.setAbout("INVITED_USER");
        // if password provided - encode; else leave temporarily null (we may generate)
        emp.setPassword(password);
        emp.setAddress("NA");
        emp.setLanguage("NA");
        emp.setBloodGroup("NA");
        emp.setCountry("NA");
        emp.setGender("NA");
        emp.setBirthday(LocalDate.now());
        emp.setLanguage("NA");
        emp.setLoginAllowed(true);
        emp.setActive(true);
        emp.setJoiningDate(LocalDate.now());
        employeeRepo.save(emp);
        mailService.sendInviteEmail(request, employeeId);
        callAuthRegister(
                emp.getEmployeeId(),
                emp.getPassword(),
                emp.getRole(),
                emp.getEmail()
        );
        return ResponseEntity.ok().body("Invite email sent");
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

    @GetMapping("/exists/{employeeId}")
    public Boolean checkEmployeeExists(
            @PathVariable("employeeId") String employeeId,
            @RequestHeader(value = "Authorization", required = false) String token
    ){
        return employeeRepository.findByEmployeeId(employeeId).isPresent();
    }

    // Lightweight meta endpoint: public (no @PreAuthorize) so other services can call without token
    @GetMapping("/meta/{employeeId}")
    public ResponseEntity<com.erp.employee_service.dto.meta.EmployeeMetaDto> meta(@PathVariable String employeeId) {
        var emp = svc.getByEmployeeId(employeeId);

        com.erp.employee_service.dto.meta.EmployeeMetaDto out = new com.erp.employee_service.dto.meta.EmployeeMetaDto();
        out.setEmployeeId(emp.getEmployeeId());
        out.setName(emp.getName());
        out.setDesignation(emp.getDesignationName());
        out.setDepartment(emp.getDepartmentName());
        out.setProfileUrl(emp.getProfilePictureUrl());

        return ResponseEntity.ok(out);
    }

    //Searching for chat
    // Search employees by name or email - public endpoint for other services
    @GetMapping("/meta/search")
    public ResponseEntity<List<EmployeeMetaDto>> searchEmployees(
            @RequestParam String query) {

        List<EmployeeResponse> employees = svc.searchEmployees(query);

        List<com.erp.employee_service.dto.meta.EmployeeMetaDto> result = employees.stream()
                .map(emp -> {
                    com.erp.employee_service.dto.meta.EmployeeMetaDto meta = new com.erp.employee_service.dto.meta.EmployeeMetaDto();
                    meta.setEmployeeId(emp.getEmployeeId());
                    meta.setName(emp.getName());
                    meta.setDesignation(emp.getDesignationName());
                    meta.setDepartment(emp.getDepartmentName());
                    meta.setProfileUrl(emp.getProfilePictureUrl());
                    return meta;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }


    /**
     * GET /api/employees/birthdays?date=2025-11-03
     * If 'date' is omitted, today's date (server local) is used.
     *
     * Accessible to authenticated users. Change @PreAuthorize if you want admin-only.
     */
    @GetMapping("/birthdays")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getBirthdays(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<EmployeeResponse> list = svc.getEmployeesWithBirthday(date);
        return ResponseEntity.ok(list);
    }

}