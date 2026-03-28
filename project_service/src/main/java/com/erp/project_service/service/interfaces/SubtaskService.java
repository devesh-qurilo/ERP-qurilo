package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.task.SubtaskDto;
import com.erp.project_service.dto.task.SubtaskCreateRequest;

import java.util.List;

public interface SubtaskService {
    SubtaskDto create(SubtaskCreateRequest req, String createdBy);
    List<SubtaskDto> listByTask(Long taskId);
    SubtaskDto update(Long id, SubtaskCreateRequest req, String updatedBy);
    void delete(Long id, String deletedBy);

    void toggleStatus(Long subtaskId, String actor);
}
