package com.erp.project_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jdk.jfr.Category;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_project", columnList = "project_id"),
        @Index(name = "idx_task_stage", columnList = "task_stage_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task extends BaseAuditable {

    @Column(name = "title", nullable = false)
    @NotBlank
    private String title;

    @ManyToOne
    @JoinColumn(name = "category_id_id")
    private TaskCategory categoryId; // or reference TaskCategory

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "no_due_date")
    private boolean noDueDate = false;

    // status stored as reference to TaskStage id (status_id)
//    @Column(name = "status_id")
//    private Long statusId;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "task_stage_id", nullable = true)
    private TaskStage taskStage;


    @Column(name = "hours_logged_minutes")
    private Long hoursLoggedMinutes = 0L; // persisted sum of minutes

    @Column(name = "completed_on")
    private LocalDate completedOn;


    // assigned employee ids
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_assigned_employees", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "employee_id", length = 50)
    private Set<String> assignedEmployeeIds = new HashSet<>();

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // many-to-many: task <-> label
    @ManyToMany
    @JoinTable(
            name = "task_labels",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private Set<Label> labels = new HashSet<>();

    @Column(name = "milestone_id")
    private Long milestoneId;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "is_private")
    private boolean isPrivate = false;

    @Column(name = "time_estimate")
    private Boolean timeEstimate = false;

    // time estimate in minutes (nullable)
    @Column(name = "time_estimate_minutes")
    private Integer timeEstimateMinutes;

    @Column(name = "is_dependent")
    private boolean isDependent = false;

    @Column(name = "dependent_task_id")
    private Long dependentTaskId;

    // attachments are stored in FileMeta (FileMeta.taskId)
    @Column(name = "duplicate_of_task_id")
    private Long duplicateOfTaskId;

    @Column(name = "status_enum", length = 20)
    private String statusEnum; // fallback/default textual status such as WAITING/INCOMPLETE

    private String createdBy;

    @CreationTimestamp
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    @Column(name="approved_by_admin")
    private Boolean approvedByAdmin = false;

    @Column(name="approved_at")
    private Instant approvedAt;

    @Column(name="approved_by")
    private String approvedBy; // admin employeeId

}
