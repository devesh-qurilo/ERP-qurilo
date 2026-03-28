package com.erp.project_service.service.impl;

import com.erp.project_service.dto.Discussion.DiscussionCategoryRequest;
import com.erp.project_service.dto.Discussion.DiscussionCategoryResponse;
import com.erp.project_service.entity.DiscussionCategory;
import com.erp.project_service.exception.ResourceNotFoundException;
import com.erp.project_service.mapper.DiscussionCategoryMapper;
import com.erp.project_service.repository.DiscussionCategoryRepository;
import com.erp.project_service.service.interfaces.DiscussionCategoryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class DiscussionCategoryServiceImpl implements DiscussionCategoryService {

    private final DiscussionCategoryRepository discussionCategoryRepository;
    private final DiscussionCategoryMapper discussionCategoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DiscussionCategoryResponse> listDiscussionCategory() {
        return discussionCategoryRepository.findAll()
                .stream()
                .map(category -> discussionCategoryMapper.toResponse(category)) // FIXED: Use lambda instead of method reference
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DiscussionCategoryResponse makeDiscussionCategory(DiscussionCategoryRequest request) {
        // Validate request
        if (request == null || request.getCategoryName() == null || request.getCategoryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }

        // Convert request to entity
        DiscussionCategory category = discussionCategoryMapper.toEntity(request);

        // Save the entity
        DiscussionCategory savedCategory = discussionCategoryRepository.save(category);

        // Convert saved entity back to response and return
        return discussionCategoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public void deleteDiscussionCategory(Long id) {
        if (!discussionCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("DiscussionCategory not found with id: " + id);
        }
        discussionCategoryRepository.deleteById(id);

    }
}