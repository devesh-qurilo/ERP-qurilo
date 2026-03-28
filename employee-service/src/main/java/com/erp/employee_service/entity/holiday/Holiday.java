package com.erp.employee_service.entity.holiday;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "holidays")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String day;

    @Column(nullable = false)
    private String occasion;

    @Column(name = "is_default_weekly")
    private Boolean isDefaultWeekly;

    @Column(name = "is_active")
    private Boolean isActive;
}