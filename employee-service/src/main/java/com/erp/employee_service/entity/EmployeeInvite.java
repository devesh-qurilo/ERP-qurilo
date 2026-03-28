package com.erp.employee_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_invites")
@Getter
@Setter
public class EmployeeInvite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String token;
    private LocalDateTime expiresAt;
    private Boolean used = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}

