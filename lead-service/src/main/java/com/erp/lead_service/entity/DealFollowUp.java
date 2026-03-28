package com.erp.lead_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "deal_followups")
@Getter
@Setter
public class DealFollowUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate nextDate; // date of followup
    private String startTime; // e.g., "15:30"

    @Column(columnDefinition = "text")
    private String remarks;

    private Boolean sendReminder = false;
    private Boolean reminderScheduled = false;

    // NEW: how long before the followup to send reminder
    // null means not configured
    private Integer remindBefore; // e.g., 1

    @Enumerated(EnumType.STRING)
    private RemindUnit remindUnit; // DAYS / HOURS / MINUTES

    // NEW: status of the followup, default PENDING
    @Enumerated(EnumType.STRING)
    private FollowupStatus status = FollowupStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
