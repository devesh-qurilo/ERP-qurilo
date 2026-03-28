package com.erp.project_service.mapper;

import com.erp.project_service.dto.Discussion.DiscussionRoomRequest;
import com.erp.project_service.dto.Discussion.DiscussionRoomResponse;
import com.erp.project_service.entity.DiscussionRoom;
import com.erp.project_service.entity.Project;
import com.erp.project_service.entity.DiscussionCategory;
import org.springframework.stereotype.Component;

@Component
public class DiscussionRoomMapper {

    public DiscussionRoom toEntity(DiscussionRoomRequest request, Project project, DiscussionCategory category, String createdBy) {
        if (request == null) {
            return null;
        }

        return DiscussionRoom.builder()
                .title(request.getTitle())
                .project(project)
                .category(category)
                .createdBy(createdBy)
                .isActive(true)
                .build();
    }

    public DiscussionRoomResponse toResponse(DiscussionRoom room) {
        if (room == null) {
            return null;
        }

        return DiscussionRoomResponse.builder()
                .id(room.getId())
                .title(room.getTitle())
                .projectId(room.getProject().getId())
                .createdBy(room.getCreatedBy())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .isActive(room.getIsActive())
                .build();
    }

    public DiscussionRoomResponse toResponseWithCategory(DiscussionRoom room) {
        if (room == null) {
            return null;
        }

        DiscussionRoomResponse response = toResponse(room);

        if (room.getCategory() != null) {
            response.setCategory(DiscussionCategoryMapper.toResponse(room.getCategory()));
        }

        return response;
    }

    public void updateFromRequest(DiscussionRoomRequest request, DiscussionRoom room, DiscussionCategory category) {
        if (request == null || room == null) {
            return;
        }

        if (request.getTitle() != null) {
            room.setTitle(request.getTitle());
        }
        if (category != null) {
            room.setCategory(category);
        }
    }
}