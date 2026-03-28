package com.erp.project_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "file_meta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileMeta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "milestone_id")
    private Long milestoneId;

    @Column(name = "recurring_task_id")
    private Long recurringTaskId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String path;

    @Column
    private String url;

    @Column(name = "mime_type")
    private String mimeType;

    private Long size;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "created_at")
    private Instant createdAt;
}
