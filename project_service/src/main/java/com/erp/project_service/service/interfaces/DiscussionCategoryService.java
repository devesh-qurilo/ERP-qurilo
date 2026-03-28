package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.Discussion.DiscussionCategoryRequest;
import com.erp.project_service.dto.Discussion.DiscussionCategoryResponse;

import java.util.List;

public interface DiscussionCategoryService {
    //Get All Category
    List<DiscussionCategoryResponse> listDiscussionCategory();

    //Make Category
    DiscussionCategoryResponse makeDiscussionCategory(DiscussionCategoryRequest request);

    //Delete Category
    void deleteDiscussionCategory(Long id);
}