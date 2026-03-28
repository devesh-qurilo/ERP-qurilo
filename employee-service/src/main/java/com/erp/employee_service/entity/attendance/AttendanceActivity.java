package com.erp.employee_service.entity.attendance;

import com.erp.employee_service.entity.Employee;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "attendance_activities", indexes = {
        @Index(name = "idx_activity_employee_date", columnList = "employee_id, date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AttendanceActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // employee who did the activity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // date for which this activity belongs (derived from time)
    @Column(name = "date", nullable = false)
    private LocalDate date;

    // IN or OUT
    @Column(name = "type", nullable = false)
    private String type; // "IN" or "OUT"

    @Column(name = "time", nullable = false)
    private LocalTime time;

    @Column(name = "location")
    private String location;

    @Column(name = "working_from")
    private String workingFrom; // Office / Home / etc.

    // Optional reference to attendance summary row if exists
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
