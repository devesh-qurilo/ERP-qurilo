package com.erp.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String employeeId; // e.g., EMP-01

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(unique = true)
    private String email; // Optional: required for admin only

    private boolean active = true;

    private String otp;

    private LocalDateTime otpExpiry;
}
