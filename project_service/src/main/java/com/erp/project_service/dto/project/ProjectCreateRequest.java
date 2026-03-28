package com.erp.project_service.dto.project;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
public class ProjectCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String shortCode;

    @NotBlank
    private String name;

    private LocalDate startDate;
    private LocalDate deadline;
    private Boolean noDeadline = false;

    private String category;
    private String department; // from employee-service
    private String clientId; // from client-service
    private MultipartFile companyFile;
    private Set<String> assignedEmployeeIds;
    private String summary;
    private Boolean tasksNeedAdminApproval = false;

    private String currency;
    @DecimalMin("0.0")
    private BigDecimal budget;
    private Integer hoursEstimate; // hours
    private Boolean allowManualTimeLogs = false;
}
