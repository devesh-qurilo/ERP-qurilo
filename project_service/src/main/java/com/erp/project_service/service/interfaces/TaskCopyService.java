package com.erp.project_service.service.interfaces;

import java.util.UUID;
public interface TaskCopyService {
    UUID createCopy(Long projectId, Long taskId, String snapshotJson, String actor);
    String getSnapshot(UUID id);
}
