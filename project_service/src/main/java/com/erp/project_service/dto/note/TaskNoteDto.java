package com.erp.project_service.dto.note;

import lombok.Data;

import java.time.Instant;

@Data
public class TaskNoteDto {
    private Long id;
    private Long taskId;
    private String title;
    private String content;
    private Boolean isPublic;
    private String ownerEmployeeId;
    private String createdBy;
    private Instant createdAt;
}
