package com.erp.project_service.service.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadAsyncService {


    private final FileServiceImpl fileServiceImpl;


    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void uploadTaskFileAsync(Long taskId, MultipartFile file, String uploadedBy) {
        try {
            log.info("Starting async file upload for task: {}", taskId);
            fileServiceImpl.uploadTaskFile(taskId, file, uploadedBy);
            log.info("Async file upload completed for task: {}", taskId);
        } catch (Exception e) {
            log.error("Async file upload failed for task {}:", taskId, e);
        }
    }
}