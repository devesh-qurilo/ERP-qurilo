package com.erp.project_service.dto.Discussion;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionMessageRequest {
    private String content;
    private Long parentMessageId;
    private MultipartFile file;
}