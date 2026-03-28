package com.erp.project_service.controller.admin;

import com.erp.project_service.dto.task.LabelDto;
import com.erp.project_service.service.interfaces.LabelService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelAdminController {

    private final LabelService svc;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")

    public ResponseEntity<LabelDto> create(@RequestBody LabelDto dto) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(201).body(svc.create(dto, actor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<LabelDto> update(@PathVariable Long id, @RequestBody LabelDto dto) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.update(id, dto, actor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("All")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<List<LabelDto>> listAll() {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(svc.listAll());
    }
}
