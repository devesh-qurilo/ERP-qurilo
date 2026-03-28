package com.erp.project_service.dto.Discussion;

import com.erp.project_service.dto.common.EmployeeMetaDto;
import lombok.*;
import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionRoomResponse {
    private Long id;
    private String title;
    private Long projectId;
    private DiscussionCategoryResponse category;
    private String createdBy;
    private EmployeeMetaDto createdByUser;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private Long messageCount;
    private DiscussionMessageResponse lastMessage;
}