package com.erp.project_service.controller.admin;

import com.erp.project_service.dto.task.TaskCategoryDto;
import com.erp.project_service.service.interfaces.TaskCategoryService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/task/task-categories")
@RequiredArgsConstructor
public class TaskCategoryAdminController {

    private final TaskCategoryService svc;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<TaskCategoryDto> create(@RequestBody java.util.Map<String,String> body) {
        String name = body.get("name");
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(201).body(svc.create(name, actor));
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> delete(@PathVariable Long id) {
//        String actor = SecurityUtils.getCurrentUserId();
//        svc.delete(id, actor);
//        return ResponseEntity.noContent().build();
//    }
@DeleteMapping("/{id}")
public ResponseEntity<?> delete(@PathVariable Long id) {
    // Manual security check
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getAuthorities().stream()
            .noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"))) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Forbidden");
        body.put("message", "Access denied");
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    String actor = SecurityUtils.getCurrentUserId();
    svc.delete(id, actor);
    return ResponseEntity.noContent().build();
}

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<TaskCategoryDto>> list() {
        return ResponseEntity.ok(svc.listAll());
    }
}
