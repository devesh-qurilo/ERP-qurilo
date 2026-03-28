package com.erp.employee_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FileMetaResponse {
    private Long id;
    private String bucket;
    private String path;
    private String filename;
    private String mime;
    private Long size;
    private String url;
    private String uploadedBy;
    private String entityType;
    private LocalDateTime uploadedAt;

    // We set this from the path param (avoid touching lazy entity graph)
    private String employeeId;
}
