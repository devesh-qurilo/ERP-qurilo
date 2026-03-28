package com.erp.employee_service.entity.pushNotification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "push_tokens", indexes = {
        @Index(name = "idx_push_tokens_user", columnList = "employee_id")
})
@Getter
@Setter
public class PushToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private String employeeId; // e.g., EMP-001

    @Column(nullable = false)
    private String provider; // "EXPO" or "FCM"

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(name = "device_info", columnDefinition = "text")
    private String deviceInfo;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen = LocalDateTime.now();
}
