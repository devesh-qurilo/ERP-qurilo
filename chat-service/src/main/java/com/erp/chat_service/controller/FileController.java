package com.erp.chat_service.controller;

import com.erp.chat_service.entity.FileAttachment;
import com.erp.chat_service.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/chat/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 7. Upload File (Independent of messages)
     * Frontend Use: Pre-upload file before sending message
     */
    @PostMapping("/upload")
    public ResponseEntity<FileAttachment> uploadFile(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam("file") MultipartFile file) {

        try {
            log.info("Uploading file: {}", file.getOriginalFilename());
            FileAttachment attachment = fileStorageService.uploadFile(file);
            return ResponseEntity.ok(attachment);
        } catch (Exception e) {
            log.error("Error uploading file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}