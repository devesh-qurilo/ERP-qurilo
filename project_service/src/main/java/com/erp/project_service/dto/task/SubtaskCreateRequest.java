package com.erp.project_service.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubtaskCreateRequest {
    @NotNull
    private Long taskId;
    @NotBlank
    private String title;
    private String description;
}
