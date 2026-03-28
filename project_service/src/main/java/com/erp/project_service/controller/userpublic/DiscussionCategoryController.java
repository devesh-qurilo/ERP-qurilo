package com.erp.project_service.controller.userpublic;

import com.erp.project_service.dto.Discussion.DiscussionCategoryRequest;
import com.erp.project_service.dto.Discussion.DiscussionCategoryResponse;
import com.erp.project_service.exception.ResourceNotFoundException;
import com.erp.project_service.service.interfaces.DiscussionCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/projects/discussion-categories")
public class DiscussionCategoryController {

    private final DiscussionCategoryService discussionCategoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<List<DiscussionCategoryResponse>> getAllCategories() {
        try {
            List<DiscussionCategoryResponse> categories = discussionCategoryService.listDiscussionCategory();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DiscussionCategoryResponse> createCategory(@RequestBody DiscussionCategoryRequest request) {
        try {
            DiscussionCategoryResponse response = discussionCategoryService.makeDiscussionCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')") // Only admin can delete
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        try {
            discussionCategoryService.deleteDiscussionCategory(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}