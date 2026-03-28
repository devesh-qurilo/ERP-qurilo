package com.erp.lead_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "deal_employees",
        uniqueConstraints = @UniqueConstraint(columnNames = {"deal_id", "employee_id"}))
@Getter
@Setter
public class DealEmployee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private String employeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id")
    private Deal deal;

    @CreationTimestamp
    private LocalDateTime assignedAt;
}
