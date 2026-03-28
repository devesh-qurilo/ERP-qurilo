package com.erp.employee_service.entity.attendance;

import com.erp.employee_service.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Employee whose attendance it is
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // date of attendance
    @Column(name = "date", nullable = false)
    private LocalDate date;

    // clock in
    @Column(name = "clock_in_time")
    private LocalTime clockInTime;

    @Column(name = "clock_in_location")
    private String clockInLocation;

    @Column(name = "clock_in_working_from")
    private String clockInWorkingFrom; // e.g., Office, Remote

    // clock out
    @Column(name = "clock_out_time")
    private LocalTime clockOutTime;

    @Column(name = "clock_out_location")
    private String clockOutLocation;

    @Column(name = "clock_out_working_from")
    private String clockOutWorkingFrom;

    @Column(name = "is_late", columnDefinition = "boolean default false")
    private Boolean late = Boolean.FALSE;

    @Column(name = "is_half_day", columnDefinition = "boolean default false")
    private Boolean halfDay = Boolean.FALSE;

    // If admin explicitly overwrote a leave/holiday to save attendance
    @Column(name = "is_overwritten", columnDefinition = "boolean default false")
    private Boolean overwritten = Boolean.FALSE;

    // explicit stored flag - when admin marks/punch saved we set true
    @Column(name = "is_present", columnDefinition = "boolean default true")
    private Boolean isPresent = Boolean.TRUE;

    // Who marked (admin) - optional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_id")
    private Employee markedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}