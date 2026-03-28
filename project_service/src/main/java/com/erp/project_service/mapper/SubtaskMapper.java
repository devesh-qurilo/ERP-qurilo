package com.erp.project_service.mapper;

import com.erp.project_service.dto.task.SubtaskCreateRequest;
import com.erp.project_service.dto.task.SubtaskDto;
import com.erp.project_service.entity.Subtask;

public final class SubtaskMapper {
    private SubtaskMapper() {}

    public static Subtask toEntity(SubtaskCreateRequest r) {
        if (r == null) return null;
        Subtask s = Subtask.builder()
                .taskId(r.getTaskId())
                .title(r.getTitle())
                .description(r.getDescription())
                .isDone(false)
                .build();
        return s;
    }

    public static SubtaskDto toDto(Subtask e) {
        if (e == null) return null;
        SubtaskDto d = new SubtaskDto();
        d.setId(e.getId());
        d.setTaskId(e.getTaskId());
        d.setTitle(e.getTitle());
        d.setDescription(e.getDescription());
        d.setIsDone(e.isDone());
        d.setCreatedBy(e.getCreatedBy());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedBy(e.getUpdatedBy());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }
}
