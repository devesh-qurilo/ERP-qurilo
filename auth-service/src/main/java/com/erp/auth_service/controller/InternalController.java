package com.erp.auth_service.controller;

import com.erp.auth_service.entity.User;
import com.erp.auth_service.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/auth")
public class InternalController {

    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder;

    public InternalController(UserRepository userRepo, BCryptPasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerInternal(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        String password = body.get("password");
        String role = body.getOrDefault("role", "ROLE_EMPLOYEE");
        String email = body.get("email"); // optional

        if (employeeId == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "employeeId & password required"));
        }

        if (userRepo.findByEmployeeId(employeeId).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("status", "error", "message", "User already exists"));
        }

        User user = new User();
        user.setEmployeeId(employeeId.trim());
        user.setPassword(encoder.encode(password));
        user.setRole(role.trim().toUpperCase());
        user.setEmail(email != null ? email.trim().toLowerCase() : null);
        user.setActive(true);

        userRepo.save(user);
        return ResponseEntity.ok(Map.of("status", "success", "message", "User registered"));
    }

    @PutMapping("/role")
    public ResponseEntity<?> updateRole(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        String role = body.get("role");

        if (employeeId == null || role == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "employeeId & role required"));
        }

        return userRepo.findByEmployeeId(employeeId.trim())
                .map(user -> {
                    user.setRole(role.trim().toUpperCase());
                    userRepo.save(user);
                    return ResponseEntity.ok(Map.of("status", "success", "message", "Role updated"));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found")));
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        String newPassword = body.get("password");

        if (employeeId == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "employeeId & password required"));
        }

        return userRepo.findByEmployeeId(employeeId.trim())
                .map(user -> {
                    user.setPassword(encoder.encode(newPassword));
                    userRepo.save(user);
                    return ResponseEntity.ok(Map.of("status", "success", "message", "Password updated"));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found")));
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<?> deleteUser(@PathVariable String employeeId) {
        return userRepo.findByEmployeeId(employeeId.trim())
                .map(user -> {
                    userRepo.delete(user);
                    return ResponseEntity.ok(Map.of("status", "success", "message", "User deleted"));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found")));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status","UP"));
    }


    @PutMapping("/email")
    public ResponseEntity<?> updateEmail(@RequestBody Map<String, String> body) {
        String employeeId = body.get("employeeId");
        String email = body.get("email");

        if (employeeId == null || email == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "employeeId & email required"));
        }

        return userRepo.findByEmployeeId(employeeId.trim())
                .map(user -> {
                    user.setEmail(email.trim().toLowerCase());
                    userRepo.save(user);
                    return ResponseEntity.ok(
                            Map.of("status", "success", "message", "Email updated"));
                })
                .orElse(
                        ResponseEntity.status(404)
                                .body(Map.of("status", "error", "message", "User not found"))
                );
    }

}
