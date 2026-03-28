package com.erp.project_service.dto.task;

import lombok.Data;

@Data
public class TaskStageDto {
    private Long id;
    private String name;
    private Integer position;
    private String labelColor;
    private Long projectId;
    private String createdBy;
}
