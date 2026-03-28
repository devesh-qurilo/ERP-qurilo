package com.erp.employee_service.service.impl;

import com.erp.employee_service.dto.CreateEmployeeRequest;
import com.erp.employee_service.dto.EmployeeResponse;
import com.erp.employee_service.dto.UpdateEmployeeRequest;
import com.erp.employee_service.dto.imports.EmployeeImportRequest;
import com.erp.employee_service.dto.settings.Profile;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.FileMeta;
import com.erp.employee_service.repository.*;
import com.erp.employee_service.service.EmployeeService;
import com.erp.employee_service.service.MailService;
import com.erp.employee_service.service.SupabaseStorageService;
import com.erp.employee_service.service.leave.LeaveQuotaService;
import com.erp.employee_service.util.EmployeeIdGenerator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository repo;
    private final DepartmentRepository deptRepo;
    private final DesignationRepository desRepo;
    private final EmployeeIdGenerator idGen;
    private final BCryptPasswordEncoder encoder;
    private final WebClient webClient;
    private final MailService mailService;
    private final SupabaseStorageService storageService;
    private final EmployeeDocumentRepository docRepo;
    private final FileMetaRepository fileRepo;
    private final LeaveQuotaService leaveQuotaService;
    private final EmergencyRepository emergencyContactRepo;

    private final LeaveQuotaRepository leaveQuotaRepo;
    private final LeaveRepository leaveRepo;
    private final NotificationRepository notificationRepo;

    private final AppreciationRepository appreciationRepo;
    private final AttendanceRepository attendanceRepo;
    private final AttendanceActivityRepository attendanceActivityRepo;



    @Value("${internal.api.key}")
    private String internalApiKey;

    public EmployeeServiceImpl(EmployeeRepository repo,
                               DepartmentRepository deptRepo,
                               DesignationRepository desRepo,
                               EmployeeIdGenerator idGen,
                               BCryptPasswordEncoder encoder,
                               WebClient.Builder webClientBuilder,
                               MailService mailService,
                               SupabaseStorageService storageService,
                               EmployeeDocumentRepository docRepo,
                               @Value("${auth.service.base-url:http://localhost:8081}") String authBaseUrl,
                               FileMetaRepository fileRepo,
                               LeaveQuotaService leaveQuotaService,
                               EmergencyRepository emergencyContactRepo,
                               LeaveQuotaRepository leaveQuotaRepo,
                               LeaveRepository leaveRepo,
                               NotificationRepository notificationRepo,
                               AppreciationRepository appreciationRepo,
                               AttendanceRepository attendanceRepo,
                               AttendanceActivityRepository attendanceActivityRepo
    ) {
        this.repo = repo;
        this.deptRepo = deptRepo;
        this.desRepo = desRepo;
        this.idGen = idGen;
        this.encoder = encoder;
        this.fileRepo = fileRepo;
        this.mailService = mailService;
        this.storageService = storageService;
        this.attendanceActivityRepo = attendanceActivityRepo;
        this.attendanceRepo = attendanceRepo;
        this.docRepo = docRepo;
        this.leaveQuotaService= leaveQuotaService;
        this.emergencyContactRepo = emergencyContactRepo;
        this.leaveQuotaRepo = leaveQuotaRepo;
        this.leaveRepo = leaveRepo;
        this.appreciationRepo = appreciationRepo;
        this.notificationRepo = notificationRepo;
        log.info("Configured external.auth.base = '{}'", authBaseUrl);

        // Normalize base URL
        if (authBaseUrl.endsWith("/")) {
            authBaseUrl = authBaseUrl.substring(0, authBaseUrl.length() - 1);
        }
        if (authBaseUrl.endsWith("/internal/auth")) {
            authBaseUrl = authBaseUrl.substring(0, authBaseUrl.length() - "/internal/auth".length());
            log.info("Stripped trailing '/internal/auth' from authBaseUrl, now '{}'", authBaseUrl);
        }
        this.webClient = webClientBuilder.baseUrl(authBaseUrl).build();
    }

    // -------------------- Auth microservice calls --------------------

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

    private void callAuthUpdateRole(String employeeId, String role) {
        try {
            webClient.put()
                    .uri("/internal/auth/role")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(java.util.Map.of("employeeId", employeeId, "role", role))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Auth role update succeeded for {}", employeeId);
        } catch (Exception ex) {
            log.error("Auth role update failed for {}: {}", employeeId, ex.getMessage());
        }
    }

    private void callAuthDelete(String employeeId) {
        try {
            webClient.delete()
                    .uri("/internal/auth/{employeeId}", employeeId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Auth delete succeeded for {}", employeeId);
        } catch (Exception ex) {
            log.error("Auth delete failed for {}: {}", employeeId, ex.getMessage());
        }
    }

    private void callAuthUpdatePassword(String employeeId, String plainPassword) {
        try {
            webClient.put()
                    .uri("/internal/auth/password")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(java.util.Map.of("employeeId", employeeId, "password", plainPassword))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Auth password update succeeded for {}", employeeId);
        } catch (Exception ex) {
            log.error("Auth password update failed for {}: {}", employeeId, ex.getMessage());
        }
    }

    // -------------------- password generator --------------------
    private String generateRandomPassword(int length) {
        SecureRandom rnd = new SecureRandom();
        byte[] bytes = new byte[length];
        rnd.nextBytes(bytes);
        String pw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        if (pw.length() > length) pw = pw.substring(0, length);
        return pw;
    }

    // -------------------- Create / Update / Delete logic --------------------

    @Override
    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request) {
        // Manual employee ID generation - check if ID already provided
        String empId = request.getEmployeeId();
        if (empId == null || empId.trim().isEmpty()) {
            empId = idGen.generateFinalId();
        } else {
            Optional<Employee> existingEmployee = repo.findByEmployeeId(empId);
            if (existingEmployee.isPresent()) {
                throw new RuntimeException("Employee ID already exists: " + empId);
            }
        }

        Employee e = new Employee();
        e.setEmployeeId(empId);
        e.setName(request.getName());
        e.setEmail(request.getEmail());

        // if password provided - encode; else leave temporarily null (we may generate)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            e.setPassword(encoder.encode(request.getPassword()));
        }

        e.setGender(request.getGender());
        e.setBirthday(request.getBirthday());
        e.setBloodGroup(request.getBloodGroup());
        e.setJoiningDate(request.getJoiningDate());
        e.setLanguage(request.getLanguage());
        e.setCountry(request.getCountry());
        e.setMobile(request.getMobile());
        e.setAddress(request.getAddress());
        e.setAbout(request.getAbout());
        e.setRole(request.getRole() == null ? "ROLE_EMPLOYEE" : request.getRole());
        e.setLoginAllowed(request.getLoginAllowed() == null ? true : request.getLoginAllowed());
        e.setReceiveEmailNotification(
                request.getReceiveEmailNotification() == null ? true : request.getReceiveEmailNotification());
        e.setSkills(request.getSkills());
        e.setHourlyRate(request.getHourlyRate());
        e.setSlackMemberId(request.getSlackMemberId());
        e.setProbationEndDate(request.getProbationEndDate());
        e.setNoticePeriodStartDate(request.getNoticePeriodStartDate());
        e.setNoticePeriodEndDate(request.getNoticePeriodEndDate());
        e.setEmploymentType(request.getEmploymentType());
        e.setMaritalStatus(request.getMaritalStatus());
        e.setBusinessAddress(request.getBusinessAddress());
        e.setOfficeShift(request.getOfficeShift());
        e.setActive(true);
        e.setCreatedAt(LocalDateTime.now());

        if (request.getDepartmentId() != null)
            deptRepo.findById(request.getDepartmentId()).ifPresent(e::setDepartment);
        if (request.getDesignationId() != null)
            desRepo.findById(request.getDesignationId()).ifPresent(e::setDesignation);
        if (request.getReportingToId() != null)
            repo.findByEmployeeId(request.getReportingToId()).ifPresent(e::setReportingTo);

        Employee saved = repo.save(e);

        // Register in Auth service if loginAllowed
        if (Boolean.TRUE.equals(saved.getLoginAllowed())) {
            try {
                String plainPassword = request.getPassword();
                if (plainPassword == null || plainPassword.isBlank()) {
                    plainPassword = generateRandomPassword(12);
                    saved.setPassword(encoder.encode(plainPassword));
                    saved = repo.save(saved);
                }
                callAuthRegister(saved.getEmployeeId(), plainPassword, saved.getRole(), saved.getEmail());

                if (Boolean.TRUE.equals(saved.getReceiveEmailNotification())) {
                    try {
                        mailService.sendWelcomeEmail(saved.getEmail(), saved.getEmployeeId(), plainPassword);
                    } catch (Exception ex) {
                        log.error("Welcome mail failed: {}", ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.error("Auth register failed: {}", ex.getMessage());
                throw new RuntimeException("Auth registration failed", ex);
            }
        } else {
            log.info("User {} created with loginAllowed=false — skipping auth registration", saved.getEmployeeId());
            if (Boolean.TRUE.equals(saved.getReceiveEmailNotification())) {
                try {
                    mailService.sendWelcomeEmail(saved.getEmail(), saved.getEmployeeId(), null);
                } catch (Exception ex) {
                    log.error("Welcome mail failed (no credentials): {}", ex.getMessage());
                }
            }
        }

        return map(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse createEmployeeWithProfile(CreateEmployeeRequest request, MultipartFile profileFile) {
        // ID generation and entity building (same as createEmployee)
        String empId = request.getEmployeeId();
        if (empId == null || empId.trim().isEmpty()) {
            empId = idGen.generateFinalId();
        } else {
            Optional<Employee> existingEmployee = repo.findByEmployeeId(empId);
            if (existingEmployee.isPresent()) {
                throw new RuntimeException("Employee ID already exists: " + empId);
            }
        }

        Employee e = new Employee();
        e.setEmployeeId(empId);
        e.setName(request.getName());
        e.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            e.setPassword(encoder.encode(request.getPassword()));
        }

        e.setGender(request.getGender());
        e.setBirthday(request.getBirthday());
        e.setBloodGroup(request.getBloodGroup());
        e.setJoiningDate(request.getJoiningDate());
        e.setLanguage(request.getLanguage());
        e.setCountry(request.getCountry());
        e.setMobile(request.getMobile());
        e.setAddress(request.getAddress());
        e.setAbout(request.getAbout());
        e.setRole(request.getRole() == null ? "ROLE_EMPLOYEE" : request.getRole());
        e.setLoginAllowed(request.getLoginAllowed() == null ? true : request.getLoginAllowed());
        e.setReceiveEmailNotification(request.getReceiveEmailNotification() == null ? true : request.getReceiveEmailNotification());
        e.setHourlyRate(request.getHourlyRate());
        e.setSlackMemberId(request.getSlackMemberId());
        e.setProbationEndDate(request.getProbationEndDate());
        e.setNoticePeriodStartDate(request.getNoticePeriodStartDate());
        e.setNoticePeriodEndDate(request.getNoticePeriodEndDate());
        e.setEmploymentType(request.getEmploymentType());
        e.setMaritalStatus(request.getMaritalStatus());
        e.setBusinessAddress(request.getBusinessAddress());
        e.setOfficeShift(request.getOfficeShift());
        e.setSkills(request.getSkills());
        e.setActive(true);
        e.setCreatedAt(LocalDateTime.now());

        if (request.getDepartmentId() != null) deptRepo.findById(request.getDepartmentId()).ifPresent(e::setDepartment);
        if (request.getDesignationId() != null) desRepo.findById(request.getDesignationId()).ifPresent(e::setDesignation);
        if (request.getReportingToId() != null) repo.findByEmployeeId(request.getReportingToId()).ifPresent(e::setReportingTo);

        // Save employee first to get ID
        Employee saved = repo.save(e);
        leaveQuotaService.assignDefaultsIfMissing(saved);
        log.info("Saved employee {} (id={}) before file upload", saved.getEmployeeId());

        // Handle profile file upload
        if (profileFile != null && !profileFile.isEmpty()) {
            try {
                log.info("Uploading profile for employee {}...", saved.getEmployeeId());

                // Use the specialized profile picture method
                FileMeta meta = storageService.uploadProfilePicture(profileFile, saved.getEmployeeId(), saved.getEmail());
                meta.setEmployee(saved);
                meta.setEntityType("PROFILE");
                FileMeta savedMeta = fileRepo.save(meta);

                saved.setProfilePictureUrl(savedMeta.getUrl());
                saved = repo.save(saved);
                log.info("Updated employee {} with profilePictureUrl={}", saved.getEmployeeId(), saved.getProfilePictureUrl());

            } catch (Exception ex) {
                log.error("Profile upload failed for employee {}: {}", saved.getEmployeeId(), ex.getMessage(), ex);
                throw new RuntimeException("Profile upload failed: " + ex.getMessage(), ex);
            }
        } else {
            log.debug("No profile file provided for employee {}", saved.getEmployeeId());
        }

        // Register in auth service (same as createEmployee)
        if (Boolean.TRUE.equals(saved.getLoginAllowed())) {
            try {
                String plainPassword = request.getPassword();
                if (plainPassword == null || plainPassword.isBlank()) {
                    plainPassword = generateRandomPassword(12);
                    saved.setPassword(encoder.encode(plainPassword));
                    saved = repo.save(saved);
                }
                callAuthRegister(saved.getEmployeeId(), plainPassword, saved.getRole(), saved.getEmail());

                if (Boolean.TRUE.equals(saved.getReceiveEmailNotification())) {
                    try {
                        mailService.sendWelcomeEmail(saved.getEmail(), saved.getEmployeeId(), plainPassword);
                    } catch (Exception ex) {
                        log.error("Welcome email failed for {}: {}", saved.getEmployeeId(), ex.getMessage(), ex);
                    }
                }
            } catch (Exception ex) {
                log.error("Auth registration failed for {}: {}", saved.getEmployeeId(), ex.getMessage(), ex);
                throw new RuntimeException("Auth registration failed: " + ex.getMessage(), ex);
            }
        } else {
            log.info("User {} created with loginAllowed=false — skipping auth registration", saved.getEmployeeId());
            if (Boolean.TRUE.equals(saved.getReceiveEmailNotification())) {
                try {
                    mailService.sendWelcomeEmail(saved.getEmail(), saved.getEmployeeId(), null);
                } catch (Exception ex) {
                    log.error("Welcome email failed for {}: {}", saved.getEmployeeId(), ex.getMessage(), ex);
                }
            }
        }
        return map(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse updateByEmployeeIdWithProfile(String employeeId, UpdateEmployeeRequest request, MultipartFile profileFile) {
        Employee e = repo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        boolean prevLoginAllowed = Boolean.TRUE.equals(e.getLoginAllowed());
        StringBuilder changes = new StringBuilder();
        String oldEmail = e.getEmail();
        // ---------- ALL FIELD UPDATES ----------
        if (request.getName() != null && !request.getName().equals(e.getName())) {
            changes.append("name: ").append(e.getName()).append(" -> ").append(request.getName()).append("\n");
            e.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(e.getEmail())) {
            changes.append("email: ").append(e.getEmail()).append(" -> ").append(request.getEmail()).append("\n");
            e.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            e.setPassword(request.getPassword());// change
            changes.append("password changed\n");
        }
        if (request.getGender() != null && !request.getGender().equals(e.getGender())) {
            e.setGender(request.getGender());
            changes.append("gender changed\n");
        }
        if (request.getBirthday() != null && !request.getBirthday().equals(e.getBirthday())) {
            e.setBirthday(request.getBirthday());
            changes.append("birthday changed\n");
        }
        if (request.getBloodGroup() != null && !request.getBloodGroup().equals(e.getBloodGroup())) {
            e.setBloodGroup(request.getBloodGroup());
            changes.append("blood group changed\n");
        }
        if (request.getJoiningDate() != null && !request.getJoiningDate().equals(e.getJoiningDate())) {
            e.setJoiningDate(request.getJoiningDate());
            changes.append("joining date changed\n");
        }
        if (request.getLanguage() != null && !request.getLanguage().equals(e.getLanguage())) {
            e.setLanguage(request.getLanguage());
            changes.append("language changed\n");
        }
        if (request.getCountry() != null && !request.getCountry().equals(e.getCountry())) {
            e.setCountry(request.getCountry());
            changes.append("country changed\n");
        }
        if (request.getMobile() != null && !request.getMobile().equals(e.getMobile())) {
            e.setMobile(request.getMobile());
            changes.append("mobile changed\n");
        }
        if (request.getAddress() != null && !request.getAddress().equals(e.getAddress())) {
            e.setAddress(request.getAddress());
            changes.append("address changed\n");
        }
        if (request.getAbout() != null && !request.getAbout().equals(e.getAbout())) {
            e.setAbout(request.getAbout());
            changes.append("about changed\n");
        }
        if (request.getDepartmentId() != null) {
            deptRepo.findById(request.getDepartmentId()).ifPresent(d -> {
                if (!d.equals(e.getDepartment())) {
                    e.setDepartment(d);
                    changes.append("department changed\n");
                }
            });
        }
        if (request.getDesignationId() != null) {
            desRepo.findById(request.getDesignationId()).ifPresent(d -> {
                if (!d.equals(e.getDesignation())) {
                    e.setDesignation(d);
                    changes.append("designation changed\n");
                }
            });
        }
        if (request.getReportingToId() != null) {
            repo.findByEmployeeId(request.getReportingToId()).ifPresent(r -> {
                if (!r.equals(e.getReportingTo())) {
                    e.setReportingTo(r);
                    changes.append("reporting to changed\n");
                }
            });
        }
        if (request.getRole() != null && !request.getRole().equals(e.getRole())) {
            e.setRole(request.getRole());
            changes.append("role changed\n");
        }
        if (request.getLoginAllowed() != null && !request.getLoginAllowed().equals(e.getLoginAllowed())) {
            e.setLoginAllowed(request.getLoginAllowed());
            changes.append("login allowed changed\n");
        }
        if (request.getReceiveEmailNotification() != null && !request.getReceiveEmailNotification().equals(e.getReceiveEmailNotification())) {
            e.setReceiveEmailNotification(request.getReceiveEmailNotification());
            changes.append("email notification preference changed\n");
        }
        if (request.getHourlyRate() != null && !request.getHourlyRate().equals(e.getHourlyRate())) {
            e.setHourlyRate(request.getHourlyRate());
            changes.append("hourly rate changed\n");
        }
        if (request.getSlackMemberId() != null && !request.getSlackMemberId().equals(e.getSlackMemberId())) {
            e.setSlackMemberId(request.getSlackMemberId());
            changes.append("slack member ID changed\n");
        }
        if (request.getSkills() != null && !request.getSkills().equals(e.getSkills())) {
            e.setSkills(request.getSkills());
            changes.append("skills changed\n");
        }
        if (request.getProbationEndDate() != null && !request.getProbationEndDate().equals(e.getProbationEndDate())) {
            e.setProbationEndDate(request.getProbationEndDate());
            changes.append("probation end date changed\n");
        }
        if (request.getNoticePeriodStartDate() != null && !request.getNoticePeriodStartDate().equals(e.getNoticePeriodStartDate())) {
            e.setNoticePeriodStartDate(request.getNoticePeriodStartDate());
            changes.append("notice period start date changed\n");
        }
        if (request.getNoticePeriodEndDate() != null && !request.getNoticePeriodEndDate().equals(e.getNoticePeriodEndDate())) {
            e.setNoticePeriodEndDate(request.getNoticePeriodEndDate());
            changes.append("notice period end date changed\n");
        }
        if (request.getEmploymentType() != null && !request.getEmploymentType().equals(e.getEmploymentType())) {
            e.setEmploymentType(request.getEmploymentType());
            changes.append("employment type changed\n");
        }
        if (request.getMaritalStatus() != null && !request.getMaritalStatus().equals(e.getMaritalStatus())) {
            e.setMaritalStatus(request.getMaritalStatus());
            changes.append("marital status changed\n");
        }
        if (request.getBusinessAddress() != null && !request.getBusinessAddress().equals(e.getBusinessAddress())) {
            e.setBusinessAddress(request.getBusinessAddress());
            changes.append("business address changed\n");
        }
        if (request.getOfficeShift() != null && !request.getOfficeShift().equals(e.getOfficeShift())) {
            e.setOfficeShift(request.getOfficeShift());
            changes.append("office shift changed\n");
        }

        // profile upload - USE THE NEW METHOD
        if (profileFile != null && !profileFile.isEmpty()) {
            try {
                log.info("Uploading new profile for employee {} ...", e.getEmployeeId());

                // Use the specialized profile picture method
                FileMeta uploadedMeta = storageService.uploadProfilePicture(
                        profileFile,
                        e.getEmployeeId(),
                        e.getEmail()
                );

                List<FileMeta> oldProfiles = fileRepo.findByEmployeeAndEntityType(e, "PROFILE");

                // Delete old FileMeta entries from database
                if (oldProfiles != null && !oldProfiles.isEmpty()) {
                    for (FileMeta oldMeta : oldProfiles) {
                        try {
                            fileRepo.delete(oldMeta);
                            log.info("Deleted old FileMeta id={}", oldMeta.getId());
                        } catch (Exception ex) {
                            log.warn("Failed to delete FileMeta id={}: {}", oldMeta.getId(), ex.getMessage());
                        }
                    }
                }

                // Create new FileMeta
                uploadedMeta.setEmployee(e);
                uploadedMeta.setEntityType("PROFILE");
                FileMeta savedMeta = fileRepo.save(uploadedMeta);
                e.setProfilePictureUrl(savedMeta.getUrl());
                changes.append("profile picture updated\n");
                log.info("Saved new FileMeta id={} url={}", savedMeta.getId(), savedMeta.getUrl());

            } catch (Exception ex) {
                log.error("Profile upload failed for {}: {}", e.getEmployeeId(), ex.getMessage(), ex);
                changes.append("profile picture upload failed\n");
                throw new RuntimeException("Profile upload failed: " + ex.getMessage(), ex);
            }
        }

        // Save final employee
        Employee saved = repo.save(e);

        // Handle loginAllowed transitions and password updates
        boolean nowLoginAllowed = Boolean.TRUE.equals(saved.getLoginAllowed());

        // false -> true : register at auth (use provided password or generate)
        if (!prevLoginAllowed && nowLoginAllowed) {
            String plainPassword = request.getPassword();
            if (plainPassword == null || plainPassword.isBlank()) {
                plainPassword = generateRandomPassword(12);
                saved.setPassword(encoder.encode(plainPassword));
                saved = repo.save(saved);
            }
            try {
                callAuthRegister(saved.getEmployeeId(), plainPassword, saved.getRole(), saved.getEmail());
                if (Boolean.TRUE.equals(saved.getReceiveEmailNotification())) {
                    try {
                        mailService.sendWelcomeEmail(saved.getEmail(), saved.getEmployeeId(), plainPassword);
                    } catch (Exception ex) {
                        log.error("Welcome mail failed: {}", ex.getMessage(), ex);
                    }
                }
            } catch (Exception ex) {
                log.error("Auth register failed for {}: {}", saved.getEmployeeId(), ex.getMessage(), ex);
            }
        }

        // true -> false : delete from auth
        if (prevLoginAllowed && !nowLoginAllowed) {
            try {
                callAuthDelete(saved.getEmployeeId());
            } catch (Exception ex) {
                log.error("Auth delete failed for {}: {}", saved.getEmployeeId(), ex.getMessage(), ex);
            }
        }

        // password changed and still loginAllowed -> update auth password
        if (request.getPassword() != null && nowLoginAllowed) {

            String plainPassword = request.getPassword();

            // employee DB -> encoded (already done)
            callAuthUpdatePassword(
                    saved.getEmployeeId(),
                    plainPassword
            );
        }

        // email changed and still loginAllowed -> update auth email
        if (request.getEmail() != null
                && !request.getEmail().equals(oldEmail)
                && nowLoginAllowed) {

            callAuthUpdateEmail(saved.getEmployeeId(), saved.getEmail());
        }

        // Send profile update email if employee wants notifications and there are changes
        if (Boolean.TRUE.equals(saved.getReceiveEmailNotification()) && changes.length() > 0) {
            try {
                mailService.sendProfileUpdateEmail(saved.getEmail(), changes.toString());
            } catch (Exception ex) {
                log.error("Profile update mail failed: {}", ex.getMessage(), ex);
            }
        }

        return map(saved);
    }

    private void callAuthUpdateEmail(String employeeId, String email) {
        try {
            webClient.put()
                    .uri("/internal/auth/email")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .bodyValue(java.util.Map.of(
                            "employeeId", employeeId,
                            "email", email
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Auth email update succeeded for {}", employeeId);
        } catch (Exception ex) {
            log.error("Auth email update failed for {}: {}", employeeId, ex.getMessage());
        }
    }


    //Profile Settings
    @Override
    public EmployeeResponse updateProfileByEmployeeIdSettings(String employeeId, Profile req, MultipartFile file) {
        Employee e = repo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        String oldEmail = e.getEmail();
        boolean prevLoginAllowed = Boolean.TRUE.equals(e.getLoginAllowed());
        StringBuilder changes = new StringBuilder();

        // ---------- ALL FIELD UPDATES ----------
        if (req.getName() != null && !req.getName().equals(e.getName())) {
            changes.append("name: ").append(e.getName()).append(" -> ").append(req.getName()).append("\n");
            e.setName(req.getName());
        }
        if (req.getEmail() != null && !req.getEmail().equals(e.getEmail())) {
            changes.append("email: ").append(e.getEmail()).append(" -> ").append(req.getEmail()).append("\n");
            e.setEmail(req.getEmail());
        }

        if (req.getGender() != null && !req.getGender().equals(e.getGender())) {
            e.setGender(req.getGender());
            changes.append("gender changed\n");
        }
        if (req.getBirthday() != null && !req.getBirthday().equals(e.getBirthday())) {
            e.setBirthday(req.getBirthday());
            changes.append("birthday changed\n");
        }
        if (req.getBloodGroup() != null && !req.getBloodGroup().equals(e.getBloodGroup())) {
            e.setBloodGroup(req.getBloodGroup());
            changes.append("blood group changed\n");
        }

        if (req.getLanguage() != null && !req.getLanguage().equals(e.getLanguage())) {
            e.setLanguage(req.getLanguage());
            changes.append("language changed\n");
        }
        if (req.getCountry() != null && !req.getCountry().equals(e.getCountry())) {
            e.setCountry(req.getCountry());
            changes.append("country changed\n");
        }
        if (req.getMobile() != null && !req.getMobile().equals(e.getMobile())) {
            e.setMobile(req.getMobile());
            changes.append("mobile changed\n");
        }
        if (req.getAddress() != null && !req.getAddress().equals(e.getAddress())) {
            e.setAddress(req.getAddress());
            changes.append("address changed\n");
        }
        if (req.getAbout() != null && !req.getAbout().equals(e.getAbout())) {
            e.setAbout(req.getAbout());
            changes.append("about changed\n");
        }
        if (req.getMaritalStatus() != null && !req.getMaritalStatus().equals(e.getMaritalStatus())) {
            e.setMaritalStatus(req.getMaritalStatus());
            changes.append("marital status changed\n");
        }
        // profile upload - USE THE NEW METHOD
        if (file != null && !file.isEmpty()) {
            try {
                log.info("Uploading new profile for employee {} ...", e.getEmployeeId());

                // Use the specialized profile picture method
                FileMeta uploadedMeta = storageService.uploadProfilePicture(
                        file,
                        e.getEmployeeId(),
                        e.getEmail()
                );

                List<FileMeta> oldProfiles = fileRepo.findByEmployeeAndEntityType(e, "PROFILE");

                // Delete old FileMeta entries from database
                if (oldProfiles != null && !oldProfiles.isEmpty()) {
                    for (FileMeta oldMeta : oldProfiles) {
                        try {
                            fileRepo.delete(oldMeta);
                            log.info("Deleted old FileMeta id={}", oldMeta.getId());
                        } catch (Exception ex) {
                            log.warn("Failed to delete FileMeta id={}: {}", oldMeta.getId(), ex.getMessage());
                        }
                    }
                }

                // Create new FileMeta
                uploadedMeta.setEmployee(e);
                uploadedMeta.setEntityType("PROFILE");
                FileMeta savedMeta = fileRepo.save(uploadedMeta);
                e.setProfilePictureUrl(savedMeta.getUrl());
                changes.append("profile picture updated\n");
                log.info("Saved new FileMeta id={} url={}", savedMeta.getId(), savedMeta.getUrl());

            } catch (Exception ex) {
                log.error("Profile upload failed for {}: {}", e.getEmployeeId(), ex.getMessage(), ex);
                changes.append("profile picture upload failed\n");
                throw new RuntimeException("Profile upload failed: " + ex.getMessage(), ex);
            }
        }

        // Save final employee
        Employee saved = repo.save(e);

        // 🔥 ADD THIS BLOCK HERE
        if (req.getEmail() != null
                && !req.getEmail().equals(oldEmail)
                && Boolean.TRUE.equals(saved.getLoginAllowed())) {

            callAuthUpdateEmail(saved.getEmployeeId(), saved.getEmail());
        }

        return map(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse updateByEmployeeId(String employeeId, UpdateEmployeeRequest request) {
        Employee e = repo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        String oldEmail = e.getEmail();
        boolean prevLoginAllowed = Boolean.TRUE.equals(e.getLoginAllowed());
        StringBuilder changes = new StringBuilder();

        // fields update (same as earlier but without file handling)
        if (request.getName() != null && !request.getName().equals(e.getName())) {
            changes.append("name: ").append(e.getName()).append(" -> ").append(request.getName()).append("\n");
            e.setName(request.getName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(e.getEmail())) {
            changes.append("email: ").append(e.getEmail()).append(" -> ").append(request.getEmail()).append("\n");
            e.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            e.setPassword(request.getPassword()); //change
            changes.append("password changed\n");
        }
        if (request.getGender() != null && !request.getGender().equals(e.getGender())) {
            e.setGender(request.getGender());
            changes.append("gender changed\n");
        }
        if (request.getBirthday() != null && !request.getBirthday().equals(e.getBirthday())) {
            e.setBirthday(request.getBirthday());
            changes.append("birthday changed\n");
        }
        if (request.getBloodGroup() != null && !request.getBloodGroup().equals(e.getBloodGroup())) {
            e.setBloodGroup(request.getBloodGroup());
            changes.append("blood group changed\n");
        }
        if (request.getJoiningDate() != null && !request.getJoiningDate().equals(e.getJoiningDate())) {
            e.setJoiningDate(request.getJoiningDate());
            changes.append("joining date changed\n");
        }
        if (request.getLanguage() != null && !request.getLanguage().equals(e.getLanguage())) {
            e.setLanguage(request.getLanguage());
            changes.append("language changed\n");
        }
        if (request.getCountry() != null && !request.getCountry().equals(e.getCountry())) {
            e.setCountry(request.getCountry());
            changes.append("country changed\n");
        }
        if (request.getMobile() != null && !request.getMobile().equals(e.getMobile())) {
            e.setMobile(request.getMobile());
            changes.append("mobile changed\n");
        }
        if (request.getAddress() != null && !request.getAddress().equals(e.getAddress())) {
            e.setAddress(request.getAddress());
            changes.append("address changed\n");
        }
        if (request.getAbout() != null && !request.getAbout().equals(e.getAbout())) {
            e.setAbout(request.getAbout());
            changes.append("about changed\n");
        }
        if (request.getDepartmentId() != null) {
            deptRepo.findById(request.getDepartmentId()).ifPresent(d -> {
                if (!d.equals(e.getDepartment())) {
                    e.setDepartment(d);
                    changes.append("department changed\n");
                }
            });
        }
        if (request.getDesignationId() != null) {
            desRepo.findById(request.getDesignationId()).ifPresent(d -> {
                if (!d.equals(e.getDesignation())) {
                    e.setDesignation(d);
                    changes.append("designation changed\n");
                }
            });
        }
        if (request.getReportingToId() != null) {
            repo.findByEmployeeId(request.getReportingToId()).ifPresent(r -> {
                if (!r.equals(e.getReportingTo())) {
                    e.setReportingTo(r);
                    changes.append("reporting to changed\n");
                }
            });
        }
        if (request.getRole() != null && !request.getRole().equals(e.getRole())) {
            e.setRole(request.getRole());
            changes.append("role changed\n");
        }
        if (request.getLoginAllowed() != null && !request.getLoginAllowed().equals(e.getLoginAllowed())) {
            e.setLoginAllowed(request.getLoginAllowed());
            changes.append("login allowed changed\n");
        }
        if (request.getReceiveEmailNotification() != null && !request.getReceiveEmailNotification().equals(e.getReceiveEmailNotification())) {
            e.setReceiveEmailNotification(request.getReceiveEmailNotification());
            changes.append("email notification preference changed\n");
        }
        if (request.getHourlyRate() != null && !request.getHourlyRate().equals(e.getHourlyRate())) {
            e.setHourlyRate(request.getHourlyRate());
            changes.append("hourly rate changed\n");
        }
        if (request.getSlackMemberId() != null && !request.getSlackMemberId().equals(e.getSlackMemberId())) {
            e.setSlackMemberId(request.getSlackMemberId());
            changes.append("slack member ID changed\n");
        }
        if (request.getSkills() != null && !request.getSkills().equals(e.getSkills())) {
            e.setSkills(request.getSkills());
            changes.append("skills changed\n");
        }
        if (request.getProbationEndDate() != null && !request.getProbationEndDate().equals(e.getProbationEndDate())) {
            e.setProbationEndDate(request.getProbationEndDate());
            changes.append("probation end date changed\n");
        }
        if (request.getNoticePeriodStartDate() != null && !request.getNoticePeriodStartDate().equals(e.getNoticePeriodStartDate())) {
            e.setNoticePeriodStartDate(request.getNoticePeriodStartDate());
            changes.append("notice period start date changed\n");
        }
        if (request.getNoticePeriodEndDate() != null && !request.getNoticePeriodEndDate().equals(e.getNoticePeriodEndDate())) {
            e.setNoticePeriodEndDate(request.getNoticePeriodEndDate());
            changes.append("notice period end date changed\n");
        }
        if (request.getEmploymentType() != null && !request.getEmploymentType().equals(e.getEmploymentType())) {
            e.setEmploymentType(request.getEmploymentType());
            changes.append("employment type changed\n");
        }
        if (request.getMaritalStatus() != null && !request.getMaritalStatus().equals(e.getMaritalStatus())) {
            e.setMaritalStatus(request.getMaritalStatus());
            changes.append("marital status changed\n");
        }
        if (request.getBusinessAddress() != null && !request.getBusinessAddress().equals(e.getBusinessAddress())) {
            e.setBusinessAddress(request.getBusinessAddress());
            changes.append("business address changed\n");
        }
        if (request.getOfficeShift() != null && !request.getOfficeShift().equals(e.getOfficeShift())) {
            e.setOfficeShift(request.getOfficeShift());
            changes.append("office shift changed\n");
        }

        if (request.getProfilePictureUrl() != null && !request.getProfilePictureUrl().equals(e.getProfilePictureUrl())) {
            e.setProfilePictureUrl(request.getProfilePictureUrl());
            changes.append("profile picture URL updated\n");
        }

        // Save final employee
        Employee saved = repo.save(e);

        // transitions
        boolean nowLoginAllowed = Boolean.TRUE.equals(saved.getLoginAllowed());

        if (!prevLoginAllowed && nowLoginAllowed) {
            String plainPassword = request.getPassword();
            if (plainPassword == null || plainPassword.isBlank()) {
                plainPassword = generateRandomPassword(12);
                saved.setPassword(encoder.encode(plainPassword));
                saved = repo.save(saved);
            }
            try {
                callAuthRegister(saved.getEmployeeId(), plainPassword, saved.getRole(), saved.getEmail());
                if (Boolean.TRUE.equals(saved.getReceiveEmailNotification())) {
                    try {
                        mailService.sendWelcomeEmail(saved.getEmail(), saved.getEmployeeId(), plainPassword);
                    } catch (Exception ex) {
                        log.error("Welcome mail failed: {}", ex.getMessage(), ex);
                    }
                }
            } catch (Exception ex) {
                log.error("Auth register failed for {}: {}", saved.getEmployeeId(), ex.getMessage(), ex);
            }
        }

        if (prevLoginAllowed && !nowLoginAllowed) {
            try {
                callAuthDelete(saved.getEmployeeId());
            } catch (Exception ex) {
                log.error("Auth delete failed for {}: {}", saved.getEmployeeId(), ex.getMessage(), ex);
            }
        }

        if (request.getPassword() != null && nowLoginAllowed) {

            String plainPassword = request.getPassword();

            // employee DB -> encoded (already done)
            callAuthUpdatePassword(
                    saved.getEmployeeId(),
                    plainPassword
            );
        }

        // email changed and still loginAllowed -> update auth email
        if (request.getEmail() != null
                && !request.getEmail().equals(oldEmail)
                && nowLoginAllowed) {

            callAuthUpdateEmail(saved.getEmployeeId(), saved.getEmail());
        }

        if (Boolean.TRUE.equals(saved.getReceiveEmailNotification()) && changes.length() > 0) {
            try {
                mailService.sendProfileUpdateEmail(saved.getEmail(), changes.toString());
            } catch (Exception ex) {
                log.error("Profile update mail failed: {}", ex.getMessage(), ex);
            }
        }

        return map(saved);
    }

    @Override
    @Transactional
    public EmployeeResponse updateRoleByEmployeeId(String employeeId, String role) {
        Employee e = repo.findByEmployeeId(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));
        e.setRole(role);
        Employee saved = repo.save(e);

        try {
            callAuthUpdateRole(saved.getEmployeeId(), role);
        } catch (Exception ex) {
            log.error("Auth role update failed: {}", ex.getMessage());
        }

        return map(saved);
    }

//    @Override
//    @Transactional
//    public void deleteByEmployeeId(String employeeId) {
//        if (!repo.existsByEmployeeId(employeeId)) {
//            log.error("Employee ID not found: {}", employeeId);
//            throw new RuntimeException("Employee not found: " + employeeId);
//        }
//
//        try {
//            callAuthDelete(employeeId);
//        } catch (Exception ex) {
//            log.error("Auth delete failed: {}", ex.getMessage());
//        }
//
//        Employee employee = repo.findByEmployeeId(employeeId).orElse(null);
//
//        try {
//            List<FileMeta> files = fileRepo.findByEmployeeEmployeeId(employeeId);
//            for (FileMeta file : files) {
//                try {
//                    if (file.getPath() != null) {
//                        storageService.deleteFile(file.getPath());
//                    }
//                } catch (Exception ex) {
//                    log.warn("Failed to delete storage file {}: {}", file.getPath(), ex.getMessage());
//                }
//            }
//            fileRepo.deleteByEmployeeEmployeeId(employeeId);
//        } catch (Exception ex) {
//            log.warn("Error while deleting file metas for {}: {}", employeeId, ex.getMessage());
//        }
//
//        // NEW: delete emergency contacts (and any other child tables)
//        try {
//            emergencyContactRepo.deleteByEmployeeEmployeeId(employeeId);
//            log.info("Deleted emergency contacts for employee {}", employeeId);
//        } catch (Exception ex) {
//            log.warn("Failed to delete emergency contacts for {}: {}", employeeId, ex.getMessage());
//            // optional: throw new RuntimeException("Could not delete emergency contacts: " + ex.getMessage(), ex);
//        }
//
//        // 3) Delete leave quota rows (fixes your current FK error)
//        try {
//            leaveQuotaRepo.deleteByEmployeeEmployeeId(employeeId);
//            log.info("Deleted leave quota entries for employee {}", employeeId);
//        } catch (Exception ex) {
//            log.warn("Failed to delete leave quota for {}: {}", employeeId, ex.getMessage());
//        }
//
//        repo.deleteByEmployeeId(employeeId);
//    }

    @Override
    @Transactional
    public void deleteByEmployeeId(String employeeId) {
        if (!repo.existsByEmployeeId(employeeId)) {
            log.error("Employee ID not found: {}", employeeId);
            throw new RuntimeException("Employee not found: " + employeeId);
        }

        try {
            callAuthDelete(employeeId);
        } catch (Exception ex) {
            log.error("Auth delete failed: {}", ex.getMessage());
        }

        Employee employee = repo.findByEmployeeId(employeeId).orElse(null);

        // 1) Delete associated documents
        try {
            List<FileMeta> files = fileRepo.findByEmployeeEmployeeId(employeeId);
            for (FileMeta file : files) {
                try {
                    if (file.getPath() != null) {
                        storageService.deleteFile(file.getPath());
                    }
                } catch (Exception ex) {
                    log.warn("Failed to delete storage file {}: {}", file.getPath(), ex.getMessage());
                }
            }
            fileRepo.deleteByEmployeeEmployeeId(employeeId);
        } catch (Exception ex) {
            log.warn("Error while deleting file metas for {}: {}", employeeId, ex.getMessage());
        }

        // 2) Delete emergency contacts
        try {
            emergencyContactRepo.deleteByEmployeeEmployeeId(employeeId);
            log.info("Deleted emergency contacts for employee {}", employeeId);
        } catch (Exception ex) {
            log.warn("Failed to delete emergency contacts for {}: {}", employeeId, ex.getMessage());
        }

        // 3) Delete leave quota rows
        try {
            leaveQuotaRepo.deleteByEmployeeEmployeeId(employeeId);
            log.info("Deleted leave quota entries for employee {}", employeeId);
        } catch (Exception ex) {
            log.warn("Failed to delete leave quota for {}: {}", employeeId, ex.getMessage());
        }

        // 4) IMPORTANT: Delete leave records first (this fixes your current error)
        try {
            // First, check if there's a leave repository
            // You need to inject LeaveRepository in EmployeeServiceImpl
            // Add: private final LeaveRepository leaveRepo;

            // 2️⃣ LEAVES (employee + approvedBy)
            leaveRepo.deleteAllForEmployee(employeeId);
            repo.flush(); // 🔥 ABSOLUTELY REQUIRED
            log.info("Deleted leave records for employee {}", employeeId);
        } catch (Exception ex) {
            log.warn("Failed to delete leave records for {}: {}", employeeId, ex.getMessage());
            // If this fails, you can't delete the employee
            throw new RuntimeException("Cannot delete employee because they have associated leave records. Please delete leaves first.");
        }

        // 5) NEW: Delete notification records (both as sender and receiver)
        try {
            notificationRepo.deleteBySenderEmployeeIdOrReceiverEmployeeId(employeeId, employeeId);
            log.info("Deleted notification records for employee {}", employeeId);
        } catch (Exception ex) {
            log.warn("Failed to delete notification records for {}: {}", employeeId, ex.getMessage());
            // If this fails, you can't delete the employee
            throw new RuntimeException("Cannot delete employee because they have associated notification records. Please delete notifications first.");
        }
        // 0) Delete appreciations FIRST
        try {
            appreciationRepo.deleteByGivenTo_EmployeeId(employeeId);
            log.info("Deleted appreciations for employee {}", employeeId);
        } catch (Exception ex) {
            log.warn("Failed to delete appreciations for {}: {}", employeeId, ex.getMessage());
            throw new RuntimeException(
                    "Cannot delete employee because appreciations exist. Please remove appreciations first."
            );
        }
        // 1️⃣ AttendanceActivity (MOST IMPORTANT)
        attendanceActivityRepo.deleteByEmployeeId(employeeId);

        // 2️⃣ Attendance
        attendanceRepo.deleteByEmployeeId(employeeId);

        // 6) Now delete the employee
        repo.deleteByEmployeeId(employeeId);
    }

    @Override
    public EmployeeResponse getByEmployeeIdOrThrow(String employeeId) {
        return getByEmployeeId(employeeId);
    }

    @Override
    public Page<EmployeeResponse> getAll(Pageable pageable) {
        Page<Employee> page = repo.findAll(pageable);
        List<EmployeeResponse> content = page.getContent().stream().map(this::map).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    public EmployeeResponse getByEmployeeId(String employeeId) {
        Employee e = repo.findByEmployeeId(employeeId).orElseThrow(() -> new RuntimeException("Employee not found"));
        return map(e);
    }

    private EmployeeResponse map(Employee e) {
        return EmployeeResponse.builder()
                .employeeId(e.getEmployeeId())
                .name(e.getName())
                .email(e.getEmail())
                .profilePictureUrl(e.getProfilePictureUrl())
                .gender(e.getGender())
                .birthday(e.getBirthday())
                .bloodGroup(e.getBloodGroup())
                .joiningDate(e.getJoiningDate())
                .language(e.getLanguage())
                .country(e.getCountry())
                .mobile(e.getMobile())
                .address(e.getAddress())
                .about(e.getAbout())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getDepartmentName() : null)
                .designationId(e.getDesignation() != null ? e.getDesignation().getId() : null)
                .designationName(e.getDesignation() != null ? e.getDesignation().getDesignationName() : null)
                .reportingToId(e.getReportingTo() != null ? e.getReportingTo().getEmployeeId() : null)
                .reportingToName(e.getReportingTo() != null ? e.getReportingTo().getName() : null)
                .role(e.getRole())
                .loginAllowed(e.getLoginAllowed())
                .receiveEmailNotification(e.getReceiveEmailNotification())
                .hourlyRate(e.getHourlyRate())
                .slackMemberId(e.getSlackMemberId())
                .skills(e.getSkills())
                .probationEndDate(e.getProbationEndDate())
                .noticePeriodStartDate(e.getNoticePeriodStartDate())
                .noticePeriodEndDate(e.getNoticePeriodEndDate())
                .employmentType(e.getEmploymentType())
                .maritalStatus(e.getMaritalStatus())
                .businessAddress(e.getBusinessAddress())
                .officeShift(e.getOfficeShift())
                .active(e.getActive())
                .createdAt(e.getCreatedAt())
                .build();
    }



    //Search Employee for chat
    @Override
    public List<EmployeeResponse> searchEmployees(String query) {
        // Search by name or email
        List<Employee> employees = repo.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);

        return employees.stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    /**
     * Return list of employees whose birthday is on the provided date.
     * If date is null, uses LocalDate.now().
     */
    @Override
    public List<EmployeeResponse> getEmployeesWithBirthday(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        int month = target.getMonthValue();
        int day = target.getDayOfMonth();

        List<Employee> employees;
        try {
            employees = repo.findByBirthdayMonthAndDay(month, day);
        } catch (Exception ex) {
            // Fallback: some DB / JPA combinations might not support MONTH/DAY in JPQL.
            // In that case fetch all and filter in-memory.
            log.warn("findByBirthdayMonthAndDay failed (falling back to in-memory filter): {}", ex.getMessage());
            employees = repo.findAll().stream()
                    .filter(e -> e.getBirthday() != null &&
                            e.getBirthday().getMonthValue() == month &&
                            e.getBirthday().getDayOfMonth() == day)
                    .collect(Collectors.toList());
        }

        return employees.stream()
                .map(this::map) // existing mapper method that returns EmployeeResponse
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Employee createEmployees(EmployeeImportRequest req) {
        // Build entity from import request (only fields from import)
        Employee employee = new Employee();

        // employeeId: use provided or generate
        String empId = req.getEmployeeId();
        if (empId == null || empId.isBlank()) {
            empId = idGen.generateFinalId();
        } else {
            // avoid collisions
            if (repo.findByEmployeeId(empId).isPresent()) {
                throw new RuntimeException("Employee ID already exists: " + empId);
            }
        }
        employee.setEmployeeId(empId);

        // Core provided fields
        employee.setName(req.getName());
        employee.setEmail(req.getEmail());
        if (req.getGender() != null) employee.setGender(req.getGender());
        if (req.getJoiningDate() != null) employee.setJoiningDate(req.getJoiningDate());
        if (req.getMobile() != null) employee.setMobile(req.getMobile());

        // ---------------------------
        // SAFE DEFAULTS for DB NOT NULL columns
        // ---------------------------
        // Password (generate and encode) — required by entity (avoid null)
        if (employee.getPassword() == null || employee.getPassword().isBlank()) {
            String plain = generateRandomPassword(12);
            employee.setPassword(encoder.encode(plain));
            // NOTE: we do not call auth registration here; createEmployees is a minimal save.
            // If you want auth registration/email on CSV import, call callAuthRegister/send mail here.
        }

        // Required textual columns — set "NA" if missing
        if (employee.getAbout() == null) employee.setAbout("NA");
        if (employee.getAddress() == null) employee.setAddress("NA");
        if (employee.getLanguage() == null) employee.setLanguage("NA");
        if (employee.getBloodGroup() == null) employee.setBloodGroup("NA");

        // Required dates — safe placeholders
        if (employee.getBirthday() == null) employee.setBirthday(LocalDate.of(1970, 1, 1));
        if (employee.getJoiningDate() == null) employee.setJoiningDate(LocalDate.now());

        // Flags & timestamps
        if (employee.getLoginAllowed() == null) employee.setLoginAllowed(true);
        if (employee.getReceiveEmailNotification() == null) employee.setReceiveEmailNotification(true);
        if (employee.getActive() == null) employee.setActive(true);
        if (employee.getCreatedAt() == null) employee.setCreatedAt(LocalDateTime.now());

        // Default role
        if (employee.getRole() == null) employee.setRole("ROLE_EMPLOYEE");

        // persist
        return repo.save(employee);
    }

    @Override
    public List<EmployeeResponse> getAllEmployee() {
        return repo.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    public EmployeeResponse getEmployeeById(String employeeId) {
        return repo.findByEmployeeId(employeeId).stream().map(this::map).findFirst().orElse(null);
    }

}