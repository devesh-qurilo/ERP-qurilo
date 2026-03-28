package com.erp.project_service.mapper;

import com.erp.project_service.dto.Discussion.DiscussionCategoryRequest;
import com.erp.project_service.dto.Discussion.DiscussionCategoryResponse;
import com.erp.project_service.entity.DiscussionCategory;
import org.springframework.stereotype.Component;

@Component
public class DiscussionCategoryMapper {

    public static DiscussionCategoryResponse toResponse(DiscussionCategory discussionCategory) {
        if (discussionCategory == null) {
            return null;
        }

        return DiscussionCategoryResponse.builder()
                .id(discussionCategory.getId())
                .categoryName(discussionCategory.getCategoryName())
                .colorCode(discussionCategory.getColorCode())
                .build();
    }

    public DiscussionCategory toEntity(DiscussionCategoryRequest request) {
        if (request == null) {
            return null;
        }

        return DiscussionCategory.builder()
                .categoryName(request.getCategoryName())
                .colorCode(request.getColorCode())
                .build();
    }

    public void updateEntityFromRequest(DiscussionCategoryRequest request, DiscussionCategory category) {
        if (request == null || category == null) {
            return;
        }

        if (request.getCategoryName() != null) {
            category.setCategoryName(request.getCategoryName());
        }
        if (request.getColorCode() != null) {
            category.setColorCode(request.getColorCode());
        }
    }
}