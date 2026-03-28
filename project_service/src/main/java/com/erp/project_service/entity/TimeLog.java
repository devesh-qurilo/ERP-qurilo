package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "time_logs", indexes = {
        @Index(name = "idx_timelog_project", columnList = "project_id"),
        @Index(name = "idx_timelog_task", columnList = "task_id"),
        @Index(name = "idx_timelog_employee", columnList = "employee_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeLog extends BaseAuditable {

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "employee_id", length = 50, nullable = false)
    private String employeeId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    // computed duration stored in minutes for convenience (service calculates)
    @Column(name = "duration_minutes")
    private Long durationMinutes;
}
