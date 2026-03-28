package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.project.*;
import com.erp.project_service.entity.Project;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProjectService {
    ProjectDto createProject(ProjectCreateRequest req, String createdBy);
    ProjectDto getProject(Long id, String requesterId);
    ProjectDto updateProject(Long id, ProjectUpdateRequest req, String updatedBy);

    @Transactional
    List<ProjectDto> getAll();

    void deleteProject(Long id, String deletedBy);
    List<ProjectDto> listProjectsForEmployee(String employeeId, int page, int size);
    void addAssignedEmployees(Long projectId, List<String> employeeIds, String actor);
    void removeAssignedEmployee(Long projectId, String employeeId, String actor);
    void updateStatus(Long projectId, String status, String actor);
    void updateProgress(Long projectId, Integer percent, String actor);
    // metrics
    com.erp.project_service.dto.project.ProjectDto getProjectWithMetrics(Long projectId, String requesterId);

    //Project Admin
    void assignProjectAdmin(Long projectId, String userIdToMakeAdmin, String actor);
    void removeProjectAdmin(Long projectId, String actor);

    List<ProjectDto> listProjectsByClient(String clientId);

    ClientProjectStatsDto getClientProjectStats(String clientId);

    EmployeeProjectCountDto getProjectCountForEmployee(String employeeId);

    ProjectCountsDto getProjectCounts();

    ProjectCountsDto getProjectCountsForEmployee(String employeeId); // NEW


    List<ProjectDto> listProjectsForEmployees(String userId);
}
