package com.erp.auth_service.controller;

import com.erp.auth_service.dto.*;
import com.erp.auth_service.entity.RefreshToken;
import com.erp.auth_service.entity.User;
import com.erp.auth_service.manualregister.ManualRegisterRequest;
import com.erp.auth_service.repository.UserRepository;
import com.erp.auth_service.security.JwtUtil;
import com.erp.auth_service.service.MailService;
import com.erp.auth_service.service.RefreshTokenService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
@ControllerAdvice
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Value("${google.api.key}")
    private String googleApiKey;

    private final WebClient webClient = WebClient.create();
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final MailService mailService;;
    private final RefreshTokenService refreshService;

    public AuthController(UserRepository userRepo, BCryptPasswordEncoder encoder, JwtUtil jwtUtil, MailService mailService, RefreshTokenService refreshService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.mailService = mailService;
        this.refreshService = refreshService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Optional<User> userOptional = userRepo.findByEmployeeId(request.getEmployeeId());

        if (userOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        User user = userOptional.get();
        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmployeeId(), List.of(user.getRole()));
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmployeeId());

        // Save refresh token in DB
        LocalDateTime expiry = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))
                .plusDays(30)
                .toLocalDateTime();
        System.out.println("Generated expiry: " + expiry); // Debug log
        refreshService.save(refreshToken, user.getEmployeeId(), expiry);
        // Build response
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setRole(user.getRole());
        response.setEmployeeId(user.getEmployeeId());

        return ResponseEntity.ok(response);
    }


    @PostMapping("/manual-register")
    public ResponseEntity<?> manualRegister(@RequestBody ManualRegisterRequest request) {
        if (userRepo.findByEmployeeId(request.getEmployeeId()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("status", "error", "message", "User already exists"));
        }
        User user = new User();
        user.setEmployeeId(request.getEmployeeId());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEmail(request.getEmail());
        user.setActive(true);
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("status", "success", "message", "User created"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequest request) {
        return userRepo.findByEmployeeId(request.getEmployeeId())
                .filter(u -> request.getEmail().equalsIgnoreCase(u.getEmail()))
                .map(u -> {
                    String otp = String.valueOf(new Random().nextInt(900000) + 100000);
                    u.setOtp(otp);
                    u.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
                    userRepo.save(u);
                    mailService.sendOtpMail(u.getEmail(), otp);
                    return ResponseEntity.ok(Map.of("status", "success", "message", "OTP sent to email"));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("status", "error", "message", "Invalid employeeId or email")));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest request) {
        return userRepo.findByEmployeeId(request.getEmployeeId())
                .filter(u -> u.getOtp() != null && u.getOtp().equals(request.getOtp()))
                .filter(u -> u.getOtpExpiry().isAfter(LocalDateTime.now()))
                .map(u -> ResponseEntity.ok(Map.of("status", "success", "message", "OTP verified")))
                .orElse(ResponseEntity.status(401).body(Map.of("status", "error", "message", "Invalid or expired OTP")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return userRepo.findByEmployeeId(request.getEmployeeId().trim())
                .filter(u -> u.getOtp() != null && u.getOtpExpiry() != null && u.getOtpExpiry().isAfter(LocalDateTime.now()))
                .map(u -> {
                    u.setPassword(encoder.encode(request.getNewPassword()));
                    u.setOtp(null);
                    u.setOtpExpiry(null);
                    userRepo.save(u);
                    return ResponseEntity.ok(Map.of("status", "success", "message", "Password reset successfully"));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("status", "error", "message", "OTP not verified or expired")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }

        Optional<RefreshToken> tokenRecord = refreshService.findValidToken(refreshToken);
        if (tokenRecord.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        try {
            Claims claims = jwtUtil.validateToken(refreshToken);
            String employeeId = claims.getSubject();

            Optional<User> userOptional = userRepo.findByEmployeeId(employeeId);
            if (userOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }

            User user = userOptional.get();
            String newAccessToken = jwtUtil.generateAccessToken(employeeId, List.of(user.getRole()));

            LoginResponse response = new LoginResponse();
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(refreshToken); // reuse existing refresh token
            response.setRole(user.getRole());
            response.setEmployeeId(employeeId);
            System.out.println("Received refresh token: " + refreshToken);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token validation failed");
        }
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token is required");
        }

        refreshService.invalidate(refreshToken);
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    //Token Based reset
    @PostMapping("/forgot-ticket")
    public ResponseEntity<?> forgotTicket(@RequestBody Map<String,String> body) {
        String employeeId = body.get("employeeId");
        String name = body.get("name");
        String designation = body.get("designation");
        String department = body.get("department");
        String email = body.get("email"); // optional

        if (employeeId == null || name == null) {
            return ResponseEntity.badRequest().body(Map.of("status","error","message","name & employeeId required"));
        }

        String subject = "ERP Password Reset Ticket: " + employeeId;
        StringBuilder b = new StringBuilder();
        b.append("Password reset ticket received:\n\n");
        b.append("Name: ").append(name).append("\n");
        if (email != null) b.append("Email: ").append(email).append("\n");
        b.append("EmployeeId: ").append(employeeId).append("\n");
        b.append("Designation: ").append(designation == null ? "-" : designation).append("\n");
        b.append("Department: ").append(department == null ? "-" : department).append("\n\n");
        b.append("Please process this ticket and update the user's password via internal APIs.");

        // send to admin(s)
        mailService.sendAdminNotification(subject, b.toString());

        return ResponseEntity.ok(Map.of("status","success","message","Ticket submitted to admin"));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status","UP"));
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translate(@RequestBody TranslateRequest request) {

        String url = "https://translation.googleapis.com/language/translate/v2?key=" + googleApiKey;

        try {
            Map<String, Object> body = Map.of(
                    "q", request.getText(),
                    "target", request.getTargetLang(),
                    "format", "text"
            );

            Map response = webClient.post()
                    .uri(url)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Map data = (Map) response.get("data");
            List translations = (List) data.get("translations");
            Map first = (Map) translations.get(0);

            return ResponseEntity.ok(
                    Map.of("translatedText", first.get("translatedText"))
            );

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Translation failed"));
        }
    }

}
