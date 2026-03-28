package com.erp.project_service.controller;

import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.dto.project.ProjectCountsDto;
import com.erp.project_service.dto.project.ProjectDto;
import com.erp.project_service.mapper.ProjectDtoEnricher;
import com.erp.project_service.service.interfaces.ProjectService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectDtoEnricher enricher;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> get(@PathVariable Long id) {
        String requester = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.getProject(id, requester));
    }

//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
//    @GetMapping
//    public ResponseEntity<List<ProjectDto>> listAssigned(@RequestParam(defaultValue = "0") int page,
//                                                         @RequestParam(defaultValue = "50") int size) {
//        String employeeId = SecurityUtils.getCurrentUserId();
//        return ResponseEntity.ok(projectService.listProjectsForEmployee(employeeId, page, size));
//    }


//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
//    @GetMapping
//    public ResponseEntity<List<ProjectDto>> listAssigneds(@RequestParam(defaultValue = "0") int page,
//                                                         @RequestParam(defaultValue = "50") int size) {
//        String userId = SecurityUtils.getCurrentUserId();
//        List<ProjectDto> list = projectService.listProjectsForEmployee(userId, page, size);
//        return ResponseEntity.ok(enricher.enrichMany(list, userId)); // 🔽
//    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping
    public ResponseEntity<List<ProjectDto>> listAssigned() {
        String userId = SecurityUtils.getCurrentUserId();
        List<ProjectDto> list = projectService.listProjectsForEmployees(userId);
        return ResponseEntity.ok(enricher.enrichMany(list, userId)); // 🔽
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/{id}/metrics")
    public ResponseEntity<ProjectDto> metrics(@PathVariable Long id) {
        String requester = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.getProjectWithMetrics(id, requester));
    }
    @GetMapping("/AllProject")
    public ResponseEntity<List<ProjectDto>> listAllProject() {
        List<ProjectDto> list = projectService.getAll();
        return ResponseEntity.ok(enricher.enrichMany(list, SecurityUtils.getCurrentUserId()));
    }

    // POST /projects/{id}/admin?userId=EMP-010
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/{id}/admin")
    public ResponseEntity<ProjectDto> assignAdmin(@PathVariable Long id, @RequestParam String userId) {
        String actor = SecurityUtils.getCurrentUserId();
        projectService.assignProjectAdmin(id, userId, actor);
        ProjectDto dto = projectService.getProject(id, actor); // service enriches
        dto.setProjectAdminId(userId);
        // try find assigned employee meta and set projectAdmin if present
        if (dto.getAssignedEmployees() != null) {
            dto.getAssignedEmployees().stream()
                    .filter(e -> userId.equals(e.getEmployeeId()))
                    .findFirst()
                    .ifPresent(e -> {
                        EmployeeMetaDto meta = new EmployeeMetaDto();
                        meta.setEmployeeId(e.getEmployeeId());
                        meta.setName(e.getName());
                        meta.setProfileUrl(e.getProfileUrl());
                        meta.setDesignation(e.getDesignation());
                        meta.setDepartment(e.getDepartment());
                        dto.setProjectAdmin(meta);
                    });
        }
        dto.setIsRequesterProjectAdmin(Boolean.TRUE); // override for response
        return ResponseEntity.ok(dto);
    }

    // DELETE /projects/{id}/admin
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}/admin")
    public ResponseEntity<ProjectDto> removeAdmin(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        projectService.removeProjectAdmin(id, actor);
        ProjectDto dto = projectService.getProject(id, actor);
        dto.setProjectAdminId(null);
        dto.setProjectAdmin(null);
        dto.setIsRequesterProjectAdmin(Boolean.FALSE); // override for response
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/counts/me")
    public ResponseEntity<ProjectCountsDto> getMyProjectCounts() {
        String me = SecurityUtils.getCurrentUserId();
        ProjectCountsDto counts = projectService.getProjectCountsForEmployee(me);
        return ResponseEntity.ok(counts);
    }

}
