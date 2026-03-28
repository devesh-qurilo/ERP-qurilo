package com.erp.employee_service.entity.notification;

import com.erp.employee_service.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // who triggered it (nullable for system)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_employee_id")
    private Employee sender;

    // who receives it
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_employee_id", nullable = false)
    private Employee receiver;

    private String title;
    @Column(length = 4000)
    private String message;
    private String type;   // e.g. LEAVE, PROMOTION, GENERIC
    private boolean readFlag = false;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime readAt;
}
