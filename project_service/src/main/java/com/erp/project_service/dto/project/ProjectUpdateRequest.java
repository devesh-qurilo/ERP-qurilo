package com.erp.project_service.dto.project;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class ProjectUpdateRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate deadline;
    private Boolean noDeadline;
    private String category;
    private String department;
    private String clientId;
    private String summary;
    private Boolean tasksNeedAdminApproval;
    private String currency;
    private BigDecimal budget;
    private Integer hoursEstimate;
    private Boolean allowManualTimeLogs;
    private MultipartFile companyFile;
    // fields added on update
    private Set<String> assignedEmployeeIds;
    private String projectStatus;
    private Integer progressPercent;
    private Boolean calculateProgressThroughTasks;
}
