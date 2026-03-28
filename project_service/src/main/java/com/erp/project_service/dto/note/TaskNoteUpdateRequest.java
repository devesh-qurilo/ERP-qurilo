package com.erp.project_service.dto.note;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskNoteUpdateRequest {

    @Size(max = 200)
    private String title;

    private String content;

    private Boolean isPublic;
}
