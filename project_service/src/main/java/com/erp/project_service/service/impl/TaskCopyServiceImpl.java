package com.erp.project_service.service.impl;

import com.erp.project_service.entity.TaskCopy;
import com.erp.project_service.repository.TaskCopyRepository;
import com.erp.project_service.service.interfaces.TaskCopyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskCopyServiceImpl implements TaskCopyService {

    private final TaskCopyRepository repo;

    @Override
    public UUID createCopy(Long projectId, Long taskId, String snapshotJson, String actor) {
        UUID id = UUID.randomUUID();
        TaskCopy tc = TaskCopy.builder()
                .id(id)
                .projectId(projectId)
                .taskId(taskId)
                .snapshotJson(snapshotJson)
                .createdAt(Instant.now())
                .build();
        repo.save(tc);
        return id;
    }

    @Override
    public String getSnapshot(UUID id) {
        return repo.findById(id).map(TaskCopy::getSnapshotJson).orElse(null);
    }
}
