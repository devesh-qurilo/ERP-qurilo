package com.erp.project_service.dto.Discussion;

import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.entity.DiscussionMessage;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionMessageResponse {
    private Long id;
    private String content;
    private Long roomId;
    private Long parentMessageId;
    private String senderId;
    private EmployeeMetaDto sender;
    private DiscussionMessage.MessageType messageType;
    private String filePath;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isDeleted;
    private String deletedBy;
    private List<DiscussionMessageResponse> replies;
    private Integer replyCount;

    // NEW FIELD
    private Boolean isBestReply;
}