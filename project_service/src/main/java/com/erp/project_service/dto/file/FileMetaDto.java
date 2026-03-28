package com.erp.project_service.dto.file;

import lombok.Data;
import java.time.Instant;

@Data
public class FileMetaDto {
    private Long id;
    private Long projectId;
    private Long taskId;
    private String filename;
    private String bucket;
    private String path;
    private String url;
    private String mimeType;
    private Long size;
    private String uploadedBy;
    private Instant createdAt;
}
