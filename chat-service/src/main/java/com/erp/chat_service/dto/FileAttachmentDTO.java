package com.erp.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachmentDTO {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String uploadedAt;
}