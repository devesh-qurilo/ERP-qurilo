package com.erp.project_service.dto.Discussion;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DiscussionCategoryResponse {
    private Long id;
    private String categoryName;
    private String colorCode;
}
