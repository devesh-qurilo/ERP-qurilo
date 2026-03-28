package com.erp.project_service.dto.project;

import com.erp.project_service.dto.common.ClientMetaDto;
import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.dto.file.FileMetaDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {
    private Long id;
    private String shortCode;
    private String name;
    private LocalDate startDate;
    private LocalDate deadline;
    private Boolean noDeadline;
    private String category;
    private String department;
    private String clientId;
    private ClientMetaDto client; // filled by service
    private String summary;
    private Boolean tasksNeedAdminApproval;
    private List<FileMetaDto> companyFiles; // filled by service
    private String currency;
    private BigDecimal budget;
    private Integer hoursEstimate;
    private Boolean allowManualTimeLogs;
    private String addedBy;
    private MultipartFile companyFile;
    private Set<String> assignedEmployeeIds;
    private List<EmployeeMetaDto> assignedEmployees; // filled by service
    private String projectStatus;
    private Integer progressPercent;
    private Boolean calculateProgressThroughTasks;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    //Project Admin
    private String projectAdminId;
    private EmployeeMetaDto projectAdmin;        // optional, filled by enricher
    private Boolean isRequesterProjectAdmin;

    // metrics - optional
    private Long totalTimeLoggedMinutes;
    private BigDecimal expenses;
    private BigDecimal profit;
    private BigDecimal earning;

    //Pinned Archieved
    // 🔽 per-user view state (frontend yahin se padhega)
    private Boolean pinned;        // null/false/true
    private Instant pinnedAt;
    private Boolean archived;
    private Instant archivedAt;
}
