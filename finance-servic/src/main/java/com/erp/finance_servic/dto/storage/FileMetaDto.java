package com.erp.finance_servic.dto.storage;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileMetaDto {
    private String bucket;
    private String path;     // internal object path (raw)
    private String filename;
    private String mime;
    private long size;
    private String url;      // public url (encoded)
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
