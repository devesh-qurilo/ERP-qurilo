package com.erp.employee_service.entity.promotion;

import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.department.Department;
import com.erp.employee_service.entity.designation.Designation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;


import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "old_department_id", nullable = false)
    private Department oldDepartment;

    @ManyToOne
    @JoinColumn(name = "old_designation_id", nullable = false)
    private Designation oldDesignation;

    @ManyToOne
    @JoinColumn(name = "new_department_id", nullable = false)
    private Department newDepartment;

    @ManyToOne
    @JoinColumn(name = "new_designation_id", nullable = false)
    private Designation newDesignation;

    @Column(nullable = false)
    private Boolean isPromotion; // true for promotion, false for demotion

    @Column(nullable = false)
    private Boolean sendNotification;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private String remarks;
}