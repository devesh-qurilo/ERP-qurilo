package com.erp.project_service.dto.task;

import lombok.Data;

import java.time.Instant;

@Data
public class SubtaskDto {
    private Long id;
    private Long taskId;
    private String title;
    private String description;
    private Boolean isDone;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;
}
