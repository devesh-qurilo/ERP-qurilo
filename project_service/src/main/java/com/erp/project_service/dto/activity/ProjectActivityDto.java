package com.erp.project_service.dto.activity;

import lombok.Data;

import java.time.Instant;

@Data
public class ProjectActivityDto {
    private Long id;
    private Long projectId;
    private String actorEmployeeId;
    private String action;
    private String metadata;
    private Instant createdAt;
}
