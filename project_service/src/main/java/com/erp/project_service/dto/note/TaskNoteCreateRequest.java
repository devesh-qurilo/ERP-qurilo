package com.erp.project_service.dto.note;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskNoteCreateRequest {
    @NotNull
    private Long taskId;
    @Size(max = 200)
    private String title;
    private String content;
    private Boolean isPublic = true;
}
