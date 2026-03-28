package com.erp.project_service.controller;

import com.erp.project_service.dto.task.TaskDto;
import com.erp.project_service.mapper.TaskDtoEnricher;
import com.erp.project_service.service.interfaces.TaskService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final TaskService taskService;
    private final TaskDtoEnricher taskEnricher; // 🔽 add


    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskDto>> myTasks() {
        String me = SecurityUtils.getCurrentUserId();
        List<TaskDto> list= taskService.listAssignedTo(me);
        return ResponseEntity.ok(taskEnricher.enrichMany(list, me));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskDto> myTask(@PathVariable Long taskId) {
        String me = SecurityUtils.getCurrentUserId();
        // service's get method will enforce that requester can access; we call with projectId unknown -> you may need a dedicated method
        // For simplicity call listAssignedTo and filter (service could provide getAssignedTask)
        List<TaskDto> all = taskService.listAssignedTo(me);
        return all.stream().filter(t -> t.getId().equals(taskId)).findFirst()
                .map(ResponseEntity::ok).orElse(ResponseEntity.status(404).build());
    }
}
