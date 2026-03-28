package com.erp.project_service.dto.task;

import com.erp.project_service.entity.TaskCategory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class TaskEmployeeCreateRequestConverter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmployeeTaskCreateRequest fromFormData(String title, Long categoryId, String projectId,
                                          String startDate, String dueDate,
                                                  String noDueDate, MultipartFile taskFile,
                                          String assignedEmployeeIds, String description,
                                          String labelIds, String milestoneId, String priority,
                                          String isPrivate, String timeEstimateMinutes, String timeEstimate,String isDependent,Long dependentTaskId) {

        EmployeeTaskCreateRequest request = new EmployeeTaskCreateRequest();

        request.setTitle(title);

        // ✅ FIX: Handle categoryId - Long type directly
        if (categoryId != null) {
            TaskCategory category = new TaskCategory();
            category.setId(categoryId);
            request.setCategoryId(category);
        }

        if (dependentTaskId != null) {
            request.setDependentTaskId(dependentTaskId);
        }

        if (StringUtils.hasText(projectId)) {
            request.setProjectId(Long.parseLong(projectId));
        }

        if (StringUtils.hasText(startDate)) {
            request.setStartDate(LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE));
        }

        if (StringUtils.hasText(dueDate)) {
            request.setDueDate(LocalDate.parse(dueDate, DateTimeFormatter.ISO_DATE));
        }

        if (StringUtils.hasText(noDueDate)) {
            request.setNoDueDate(Boolean.parseBoolean(noDueDate));
        }

        request.setTaskFile(taskFile);

        if (StringUtils.hasText(assignedEmployeeIds)) {
            try {
                Set<String> employeeIds = objectMapper.readValue(assignedEmployeeIds, new TypeReference<Set<String>>() {});
                request.setAssignedEmployeeIds(employeeIds);
            } catch (JsonProcessingException e) {
                // Fallback: try comma-separated values
                Set<String> employeeIds = new HashSet<>();
                for (String id : assignedEmployeeIds.split(",")) {
                    if (StringUtils.hasText(id.trim())) {
                        employeeIds.add(id.trim());
                    }
                }
                request.setAssignedEmployeeIds(employeeIds);
            }
        }

        request.setDescription(description);

        if (StringUtils.hasText(labelIds)) {
            try {
                Set<Long> labels = objectMapper.readValue(labelIds, new TypeReference<Set<Long>>() {});
                request.setLabelIds(labels);
                log.info("Parsed labelIds: {}", labels); // ✅ Add this log
            } catch (JsonProcessingException e) {
                // Fallback: try comma-separated values
                Set<Long> labels = new HashSet<>();
                for (String id : labelIds.split(",")) {
                    if (StringUtils.hasText(id.trim())) {
                        labels.add(Long.parseLong(id.trim()));
                    }
                }
                request.setLabelIds(labels);
                log.info("Fallback parsed labelIds: {}", labels); // ✅ Add this log
            }
        }

        if (StringUtils.hasText(milestoneId)) {
            request.setMilestoneId(Long.parseLong(milestoneId));
        }

        request.setPriority(priority);

        if (StringUtils.hasText(isPrivate)) {
            request.setIsPrivate(Boolean.parseBoolean(isPrivate));
        }
        if (StringUtils.hasText(timeEstimate)) {
            request.setTimeEstimate(Boolean.parseBoolean(timeEstimate));
        }

        if (StringUtils.hasText(timeEstimateMinutes)) {
            request.setTimeEstimateMinutes(Integer.parseInt(timeEstimateMinutes));
        }

        // ✅ FIXED: parse isDependent into the correct DTO field
        if (StringUtils.hasText(isDependent)) {
            request.setIsDependent(Boolean.parseBoolean(isDependent));
        }

        return request;
    }
}
