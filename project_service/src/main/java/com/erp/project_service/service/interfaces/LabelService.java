package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.task.LabelDto;

import java.util.List;

public interface LabelService {
    LabelDto create(LabelDto dto, String createdBy);
    LabelDto update(Long id, LabelDto dto, String updatedBy);
    void delete(Long id, String deletedBy);
    List<LabelDto> listByProject(Long projectId);

    List<LabelDto> listAll();
}
