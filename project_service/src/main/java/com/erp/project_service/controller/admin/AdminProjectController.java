package com.erp.project_service.controller.admin;

import com.erp.project_service.dto.EmployeeAssignmentRequest;
import com.erp.project_service.dto.Import.ImportResult;
import com.erp.project_service.dto.project.*;
import com.erp.project_service.entity.Project;
import com.erp.project_service.mapper.ProjectDtoEnricher;
import com.erp.project_service.service.interfaces.ProjectCsvImportService;
import com.erp.project_service.service.interfaces.ProjectService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminProjectController {

    private final ProjectService projectService;
    private final ProjectDtoEnricher enricher;


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectDto> create(
            @RequestParam("shortCode") String shortCode,
            @RequestParam("projectName") String projectName,
            @RequestParam("startDate") String startDate, // ✅ String mein lo
            @RequestParam("deadline") String deadline,   // ✅ String mein lo
            @RequestParam("noDeadline") boolean noDeadline,
            @RequestParam("projectCategory") String projectCategory,
            @RequestParam("department") String department,
            @RequestParam("clientId") String clientId,
            @RequestParam("projectSummary") String projectSummary,
            @RequestParam("tasksNeedAdminApproval") boolean tasksNeedAdminApproval,
            @RequestParam("currency") String currency,
            @RequestParam("projectBudget") BigDecimal projectBudget,
            @RequestParam("hoursEstimate") Integer hoursEstimate,
            @RequestParam("allowManualTimeLogs") boolean allowManualTimeLogs,
            @RequestParam("assignedEmployeeIds") String assignedEmployeeIds, // ✅ Comma-separated string
            @RequestParam(value = "companyFile", required = false) MultipartFile companyFile) {

        String actor = SecurityUtils.getCurrentUserId();

        // Parse comma-separated employee IDs
        Set<String> assignedEmployeeIdsSet = parseCommaSeparatedIds(assignedEmployeeIds);

        // Convert String dates to LocalDate
        LocalDate startDateParsed = LocalDate.parse(startDate);
        LocalDate deadlineParsed = LocalDate.parse(deadline);

        ProjectCreateRequest req = ProjectCreateRequest.builder()
                .shortCode(shortCode)
                .name(projectName)
                .startDate(startDateParsed)
                .deadline(deadlineParsed)
                .noDeadline(noDeadline)
                .category(projectCategory)
                .department(department)
                .clientId(clientId)
                .summary(projectSummary)
                .tasksNeedAdminApproval(tasksNeedAdminApproval)
                .currency(currency)
                .budget(projectBudget)
                .hoursEstimate(hoursEstimate)
                .allowManualTimeLogs(allowManualTimeLogs)
                .companyFile(companyFile)
                .assignedEmployeeIds(assignedEmployeeIdsSet)
                .build();

        ProjectDto dto = projectService.createProject(req, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    private Set<String> parseCommaSeparatedIds(String employeeIds) {
        if (employeeIds == null || employeeIds.trim().isEmpty()) {
            return new HashSet<>();
        }

        // Remove any JSON artifacts if present
        String cleanIds = employeeIds.replaceAll("[\\[\\]\"]", "");

        return Arrays.stream(cleanIds.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> get(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(projectService.getProject(id, actor));
    }

    // For basic project updates (without file)
// UPDATE PROJECT (WITH ALL FIELDS SUPPORT)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectDto> update(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "deadline", required = false) String deadline,
            @RequestParam(value = "noDeadline", required = false) Boolean noDeadline,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "clientId", required = false) String clientId,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestParam(value = "tasksNeedAdminApproval", required = false) Boolean tasksNeedAdminApproval,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam(value = "budget", required = false) BigDecimal budget,
            @RequestParam(value = "hoursEstimate", required = false) Integer hoursEstimate,
            @RequestParam(value = "allowManualTimeLogs", required = false) Boolean allowManualTimeLogs,
            @RequestParam(value = "assignedEmployeeIds", required = false) String assignedEmployeeIds,
            @RequestParam(value = "projectStatus", required = false) String projectStatus,
            @RequestParam(value = "progressPercent", required = false) Integer progressPercent,
            @RequestParam(value = "calculateProgressThroughTasks", required = false) Boolean calculateProgressThroughTasks,
            @RequestParam(value = "companyFile", required = false) MultipartFile companyFile) {

        String actor = SecurityUtils.getCurrentUserId();

        ProjectUpdateRequest req = new ProjectUpdateRequest();

        // Set all fields from request parameters
        if (name != null) req.setName(name);
        if (startDate != null) req.setStartDate(LocalDate.parse(startDate));
        if (deadline != null) req.setDeadline(LocalDate.parse(deadline));
        if (noDeadline != null) req.setNoDeadline(noDeadline);
        if (category != null) req.setCategory(category);
        if (department != null) req.setDepartment(department);
        if (clientId != null) req.setClientId(clientId);
        if (summary != null) req.setSummary(summary);
        if (tasksNeedAdminApproval != null) req.setTasksNeedAdminApproval(tasksNeedAdminApproval);
        if (currency != null) req.setCurrency(currency);
        if (budget != null) req.setBudget(budget);
        if (hoursEstimate != null) req.setHoursEstimate(hoursEstimate);
        if (allowManualTimeLogs != null) req.setAllowManualTimeLogs(allowManualTimeLogs);
        if (assignedEmployeeIds != null) {
            req.setAssignedEmployeeIds(parseCommaSeparatedIds(assignedEmployeeIds));
        }
        if (projectStatus != null) req.setProjectStatus(projectStatus);
        if (progressPercent != null) req.setProgressPercent(progressPercent);
        if (calculateProgressThroughTasks != null) req.setCalculateProgressThroughTasks(calculateProgressThroughTasks);
        if (companyFile != null) req.setCompanyFile(companyFile);

        ProjectDto dto = projectService.updateProject(id, req, actor);
        return ResponseEntity.ok(dto);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        String actor = SecurityUtils.getCurrentUserId();
        projectService.deleteProject(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<?> addAssigned(@PathVariable Long id, @RequestBody EmployeeAssignmentRequest request) {
        String actor = SecurityUtils.getCurrentUserId();
        projectService.addAssignedEmployees(id, request.getEmployeeIds(), actor);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/assign/{employeeId}")
    public ResponseEntity<?> removeAssigned(@PathVariable Long id, @PathVariable String employeeId) {
        String actor = SecurityUtils.getCurrentUserId();
        projectService.removeAssignedEmployee(id, employeeId, actor);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        String actor = SecurityUtils.getCurrentUserId();
        projectService.updateStatus(id, status, actor);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable Long id, @RequestParam Integer percent) {
        String actor = SecurityUtils.getCurrentUserId();
        projectService.updateProgress(id, percent, actor);
        return ResponseEntity.ok().build();
    }

//    @GetMapping
//    public ResponseEntity<List<ProjectDto>> listAll() {
//        String userId = SecurityUtils.getCurrentUserId();
//        List<ProjectDto> list = projectService.listProjectsForEmployee(userId, 0, 1000);
//        return ResponseEntity.ok(enricher.enrichMany(list, userId)); // 🔽
//    }

    @GetMapping
    public ResponseEntity<List<ProjectDto>> listAll() {
        String userId = SecurityUtils.getCurrentUserId();
        List<ProjectDto> list = projectService.getAll();
        return ResponseEntity.ok(enricher.enrichMany(list, userId)); // 🔽
    }

    @GetMapping("/AllProject")
    public ResponseEntity<List<ProjectDto>> listAllProject() {
        List<ProjectDto> list = projectService.getAll();
        return ResponseEntity.ok(enricher.enrichMany(list, SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> listProjectsForEmployee(
            @PathVariable String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            List<ProjectDto> projects = projectService.listProjectsForEmployee(employeeId, page, size);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to fetch projects: " + e.getMessage());
        }
    }

    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ProjectDto>> listProjectsByClient(@PathVariable String clientId) {
        try {
            List<ProjectDto> projects = projectService.listProjectsByClient(clientId);
            // reuse existing enricher to attach client/employee meta + files
            return ResponseEntity.ok(enricher.enrichMany(projects, SecurityUtils.getCurrentUserId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // add field to controller (constructor injection with @RequiredArgsConstructor will pick it up)
    private final ProjectCsvImportService projectCsvImportService;

    // new endpoint
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImportResult>> importProjectsCsv(
            @RequestPart("file") MultipartFile file) {

        String actor = SecurityUtils.getCurrentUserId();
        List<ImportResult> res = projectCsvImportService.importProjectsFromCsv(file, actor);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/client/{clientId}/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ClientProjectStatsDto> getClientProjectStats(@PathVariable String clientId) {
        try {
            ClientProjectStatsDto stats = projectService.getClientProjectStats(clientId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/employee/{employeeId}/stats/count")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeeProjectCountDto> getProjectCountForEmployee(@PathVariable String employeeId) {
        try {
            EmployeeProjectCountDto dto = projectService.getProjectCountForEmployee(employeeId);
            return ResponseEntity.ok(dto);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/counts")
    public ResponseEntity<ProjectCountsDto> getProjectCounts() {
        ProjectCountsDto counts = projectService.getProjectCounts();
        return ResponseEntity.ok(counts);
    }

}
