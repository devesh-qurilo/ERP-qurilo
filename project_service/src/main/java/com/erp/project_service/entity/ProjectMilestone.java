package com.erp.project_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "project_milestones", indexes = {
        @Index(name = "idx_milestone_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMilestone extends BaseAuditable {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "title", nullable = false)
    @NotBlank
    private String title;

    @Column(name = "milestone_cost", precision = 19, scale = 2)
    private BigDecimal milestoneCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MilestoneStatus status = MilestoneStatus.INCOMPLETE;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
