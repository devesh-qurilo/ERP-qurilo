package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_copy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskCopy {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(columnDefinition = "text")
    private String snapshotJson;

    @Column(name = "created_at")
    private Instant createdAt;
}
