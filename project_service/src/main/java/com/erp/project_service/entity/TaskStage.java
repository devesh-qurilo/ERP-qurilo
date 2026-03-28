package com.erp.project_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_stages", indexes = {
        @Index(name = "idx_taskstage_project", columnList = "project_id"),
        @Index(name = "idx_taskstage_position", columnList = "position")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStage extends BaseAuditable {

    @Column(name = "name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "position")
    private Integer position;

    @Column(name = "label_color", length = 50)
    private String labelColor;

    // projectId nullable: if null -> global stage
    @Column(name = "project_id")
    private Long projectId;

    private String createdBy;
    @CreationTimestamp
    private LocalDateTime createdDate;
}
