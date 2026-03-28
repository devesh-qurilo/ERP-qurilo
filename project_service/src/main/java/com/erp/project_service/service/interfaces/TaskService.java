package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.task.*;

import java.util.List;

public interface TaskService {
    TaskDto create(Long projectId, TaskCreateRequest req, String createdBy);
    TaskDto creates(Long projectId, EmployeeTaskCreateRequest req, String createdBy);
    TaskDto get(Long projectId, Long taskId, String requesterId);
    TaskDto update(Long projectId, Long taskId, TaskCreateRequest req, String updatedBy);
    void delete(Long projectId, Long taskId, String deletedBy);
    List<TaskDto> listByProject(Long projectId);
    List<TaskDto> listAssignedTo(String employeeId);
    TaskDto changeStatus(Long projectId, Long taskId, Long statusId, String actor);
    TaskDto duplicate(Long projectId, Long taskId, String actor);
    TaskDto approveWaitingTask(Long projectId, Long taskId, String approver);
    List<TaskDto> getAll();
    void deleteTaskById(Long taskId, String actor);
    List<TaskDto> getAllWaititngTask(String status);

    EmployeeTaskCountDto getAssignedTaskCount(String employeeId);

    TaskCountsDto getAllTasksCounts(); // admin global counts
    TaskCountsDto getTasksCountsForEmployee(String employeeId); // employee scoped

    TaskDto getByTaskId(Long taskId);
}
