package com.erp.client_service.dto.document;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponseDto {
    private Long id;
    private String filename;
    private String url;
    private String mimeType;
    private Long size;
    private String uploadedAt;
    private String uploadedBy;
}
