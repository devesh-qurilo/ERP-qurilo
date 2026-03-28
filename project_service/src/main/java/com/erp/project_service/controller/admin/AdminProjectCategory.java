package com.erp.project_service.controller.admin;

import com.erp.project_service.entity.ProjectCategory;
import com.erp.project_service.repository.ProjectCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/category")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminProjectCategory {

    private final ProjectCategoryRepository projectCategoryRepository;

    //Adding Category
    @PostMapping
    public ResponseEntity<ProjectCategory> save(@RequestBody ProjectCategory projectCategory) {
        return ResponseEntity.ok(projectCategoryRepository.save(projectCategory));
    }

    //Getting Category
    @GetMapping
    public ResponseEntity<List<ProjectCategory>> findAll() {
        return ResponseEntity.ok(projectCategoryRepository.findAll());
    }

    //deleting Category
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteById(@PathVariable Long id) {
        projectCategoryRepository.deleteById(id);
        return ResponseEntity.ok().body("delete successfully");
    }
}
