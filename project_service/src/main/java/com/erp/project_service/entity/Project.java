package com.erp.project_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_project_shortcode", columnList = "short_code"),
        @Index(name = "idx_project_status", columnList = "project_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseAuditable {

    @Column(name = "short_code", length = 50, nullable = false, unique = true)
    @NotBlank
    private String shortCode;

    @Column(name = "name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Column(name = "no_deadline")
    private boolean noDeadline = false;

    @Column(name = "category")
    private String category;

    // references: employee-service and client-service will be used to enrich metadata at runtime
    @Column(name = "department_id")
    private String department;  // reference to employee-service department id

    @Column(name = "client_id")
    private String clientId; // reference to client-service client id

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "tasks_need_admin_approval")
    private boolean tasksNeedAdminApproval = false;

    // project-level files stored in FileMeta (see FileMeta.projectId)
    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "budget", precision = 19, scale = 2)
    private BigDecimal budget;

    @Column(name = "hours_estimate")
    private Integer hoursEstimate; // in hours

    @Column(name = "allow_manual_time_logs")
    private boolean allowManualTimeLogs = false;

    // addedBy is same as createdBy, but explicitly keep for clarity if needed
    @Column(name = "added_by", length = 50)
    private String addedBy;

    // assigned employees list as element collection of employeeId strings
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_assigned_employees",
            joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "employee_id", length = 50)
    private Set<String> assignedEmployeeIds = new HashSet<>();

    @Column(name = "project_admin_id")
    private String projectAdminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", length = 20)
    private ProjectStatus projectStatus = ProjectStatus.NOT_STARTED;

    @Column(name = "progress_percent")
    private Integer progressPercent = 0; // 0-100

    @Column(name = "calculate_progress_through_tasks")
    private boolean calculateProgressThroughTasks = false;

    // metrics fields are transient / computed but we keep optional stored fields for caching if needed
    @Column(name = "cached_total_time_logged")
    private Long cachedTotalTimeLogged; // in minutes or hours as decided

    @Column(name = "cached_expenses", precision = 19, scale = 2)
    private BigDecimal cachedExpenses;

    @Column(name = "cached_profit", precision = 19, scale = 2)
    private BigDecimal cachedProfit;
}
