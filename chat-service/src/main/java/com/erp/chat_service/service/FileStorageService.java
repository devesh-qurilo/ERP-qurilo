package com.erp.chat_service.service;

import com.erp.chat_service.entity.FileAttachment;
import com.erp.chat_service.repository.FileAttachmentRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class FileStorageService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private FileAttachmentRepository fileAttachmentRepository;

    @Value("${project.supabase.bucket:ERP-BUCKET}")
    private String bucket;

    @Value("${project.supabase.url}")
    private String supabaseUrl;

    @Value("${chat.attachments.enabled:true}")
    private boolean fileUploadEnabled;

    public FileAttachment uploadFile(MultipartFile file) {
        log.info("=== File Upload Started ===");
        log.info("File: {}, Size: {}, Type: {}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // Always create file attachment entity first
        FileAttachment attachment = createFileAttachmentEntity(file);

        try {
            if (!fileUploadEnabled) {
                log.warn("File upload disabled. Using mock storage.");
                return saveMockAttachment(attachment);
            }

            // Generate unique file path in Supabase
            String fileExtension = getFileExtension(file.getOriginalFilename());
            String uniqueFileName = UUID.randomUUID() + fileExtension;
            String filePath = "chat-files/" + uniqueFileName; // Store in chat-files folder within ERP-BUCKET

            log.info("Uploading to Supabase - Bucket: {}, Path: {}", bucket, filePath);

            // ---- CHANGE #1: PutObjectRequest + streaming upload (no in-memory byte[] load) ----
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .build();

            try (InputStream in = file.getInputStream()) {
                s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(in, file.getSize()));
            }

            log.info("✅ File uploaded successfully to Supabase");

            // Generate public URL (works only if bucket is PUBLIC)
            String fileUrl = generatePublicFileUrl(filePath);
            log.info("Generated file URL: {}", fileUrl);

            // Update attachment with real Supabase URL
            attachment.setFileUrl(fileUrl);
            attachment.setSupabasePath(filePath);

            FileAttachment savedAttachment = fileAttachmentRepository.save(attachment);
            log.info("✅ File attachment saved to database with ID: {}", savedAttachment.getId());

            return savedAttachment;

        } catch (Exception e) {
            log.error("❌ File upload to Supabase failed: {}", e.getMessage(), e);

            // Fallback: save as mock attachment
            log.warn("Using fallback mock attachment due to upload failure");
            return saveMockAttachment(attachment);
        }
    }

    private FileAttachment createFileAttachmentEntity(MultipartFile file) {
        FileAttachment attachment = new FileAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        return attachment;
    }

    private FileAttachment saveMockAttachment(FileAttachment attachment) {
        attachment.setFileUrl("mock://upload-disabled/" + attachment.getFileName());
        attachment.setSupabasePath("mock-path/" + UUID.randomUUID());
        return fileAttachmentRepository.save(attachment);
    }

    // ---- CHANGE #2: Public URL helper (bucket must be PUBLIC in Supabase) ----
    private String generatePublicFileUrl(String filePath) {
        // This works only if the bucket is PUBLIC in Supabase Storage
        return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucket, filePath);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
