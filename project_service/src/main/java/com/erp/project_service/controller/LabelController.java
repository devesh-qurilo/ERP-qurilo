package com.erp.project_service.controller;

import com.erp.project_service.dto.task.LabelDto;
import com.erp.project_service.service.interfaces.LabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService svc;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<LabelDto>> list(@PathVariable Long projectId) {
        return ResponseEntity.ok(svc.listByProject(projectId));
    }


}
