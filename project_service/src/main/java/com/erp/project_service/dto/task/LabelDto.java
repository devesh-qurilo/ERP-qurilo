package com.erp.project_service.dto.task;

import lombok.Data;

@Data
public class LabelDto {
    private Long id;
    private String name;
    private String colorCode;
    private Long projectId;
    private String projectName;
    private String description;
    private String createdBy;
}
