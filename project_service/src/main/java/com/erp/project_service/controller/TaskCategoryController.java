package com.erp.project_service.controller;

import com.erp.project_service.dto.task.TaskCategoryDto;
import com.erp.project_service.service.interfaces.TaskCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task-categories")
@RequiredArgsConstructor
public class TaskCategoryController {

    private final TaskCategoryService svc;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<TaskCategoryDto>> list() {
        return ResponseEntity.ok(svc.listAll());
    }
}
