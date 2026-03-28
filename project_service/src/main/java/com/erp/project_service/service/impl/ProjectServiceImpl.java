//package com.erp.project_service.service.impl;
//
//import com.erp.project_service.client.ClientClient;
//import com.erp.project_service.client.EmployeeClient;
//import com.erp.project_service.dto.common.ClientMetaDto;
//import com.erp.project_service.dto.common.EmployeeMetaDto;
//import com.erp.project_service.dto.file.FileMetaDto;
//import com.erp.project_service.dto.project.*;
//import com.erp.project_service.entity.FileMeta;
//import com.erp.project_service.entity.Project;
//import com.erp.project_service.entity.ProjectStatus;
//import com.erp.project_service.exception.DuplicateResourceException;
//import com.erp.project_service.exception.NotFoundException;
//import com.erp.project_service.mapper.FileMetaMapper;
//import com.erp.project_service.mapper.ProjectMapper;
//import com.erp.project_service.repository.FileMetaRepository;
//import com.erp.project_service.repository.ProjectMilestoneRepository;
//import com.erp.project_service.repository.ProjectRepository;
//import com.erp.project_service.repository.TimeLogRepository;
//import com.erp.project_service.service.interfaces.FileService;
//import com.erp.project_service.service.interfaces.ProjectService;
//import com.erp.project_service.service.interfaces.ProjectActivityService;
//import com.erp.project_service.service.notification.NotificationHelperService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ProjectServiceImpl implements ProjectService {
//
//    private final ProjectRepository projectRepository;
//    private final FileMetaRepository fileMetaRepository;
//    private final EmployeeClient employeeClient;
//    private final ClientClient clientClient;
//    private final ProjectActivityService activityService;
//    private final TimeLogRepository timeLogRepository;
//    private final ProjectMilestoneRepository projectMilestoneRepository;
//    private final FileService fileService;
//    private final NotificationHelperService notificationHelper; // NEW
//    @Override
//    @Transactional
//    public ProjectDto createProject(ProjectCreateRequest req, String createdBy) {
//        if (req.getShortCode() != null && projectRepository.existsByShortCode(req.getShortCode())) {
//            throw new DuplicateResourceException("Project shortCode '" + req.getShortCode() + "' already exists");
//        }
//    // Create project first
//    Project p = ProjectMapper.toEntity(req);
//    p.setCreatedBy(createdBy);
//    p.setAddedBy(createdBy);
//    Project saved = projectRepository.save(p);
//
//    // Handle file upload if present
//    if (req.getCompanyFile() != null && !req.getCompanyFile().isEmpty()) {
//        uploadCompanyFile(req.getCompanyFile(), saved.getId(), createdBy);
//    }
//
//        // NEW: Send custom project creation notifications
//        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
//            System.out.println("DEBUG: Type of assignedEmployeeIds: " + req.getAssignedEmployeeIds().getClass().getName());
//            List<String> employeeIds = new ArrayList<>(req.getAssignedEmployeeIds());
//            System.out.println("DEBUG: After conversion to List: " + employeeIds.getClass().getName());
//            sendProjectAssignmentNotifications(createdBy, employeeIds, req.getName(), saved.getId(), "PROJECT CREATED");
//        }
//
//    activityService.record(saved.getId(), createdBy, "PROJECT_CREATED", null);
//    return enrich(ProjectMapper.toDto(saved));
//}
//
//    private void uploadCompanyFile(MultipartFile companyFile, Long projectId, String uploadedBy) {
//        try {
//            // Use existing file service to upload
//            fileService.uploadProjectFile(projectId, companyFile, uploadedBy);
//        } catch (Exception e) {
//            // Log error but don't fail project creation
//            System.err.println("Failed to upload company file: " + e.getMessage());
//            // You can throw exception here if you want to fail project creation on file upload failure
//            // throw new RuntimeException("Failed to upload company file", e);
//        }
//    }
//
//
//    @Override
//    public ProjectDto getProject(Long id, String requesterId) {
//        Project p = projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
//        return enrich(ProjectMapper.toDto(p));
//    }
//
//    @Override
//    @Transactional
//    public ProjectDto updateProject(Long id, ProjectUpdateRequest req, String updatedBy) {
//        Project p = projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
//
//        // Apply all updates using ProjectMapper
//        ProjectMapper.applyUpdate(req, p);
//        p.setUpdatedBy(updatedBy);
//
//        Project saved = projectRepository.save(p);
//
//        // Handle file upload if present during update
//        if (req.getCompanyFile() != null && !req.getCompanyFile().isEmpty()) {
//            uploadCompanyFile(req.getCompanyFile(), saved.getId(), updatedBy);
//        }
//
//        // NEW: Send project update notifications
////        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
////            sendProjectAssignmentNotifications(updatedBy, (List<String>) req.getAssignedEmployeeIds(), req.getName(), saved.getId(), "UPDATED");
////        }
//        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
//            List<String> employeeIds = new ArrayList<>(req.getAssignedEmployeeIds());
//            sendProjectAssignmentNotifications(updatedBy, employeeIds, req.getName(), saved.getId(), "PROJECT CREATED");
//        }
//
//        activityService.record(saved.getId(), updatedBy, "PROJECT_UPDATED", null);
//        return enrich(ProjectMapper.toDto(saved));
//    }
//
//    @Transactional
//    @Override
//    public List<ProjectDto> getAll(){
//
//        return projectRepository.findAll()
//                .stream()
//                .map(ProjectMapper::toDto)
//                .map(this::enrich)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public void deleteProject(Long id, String deletedBy) {
//        // ✅ FIX: First get the project to check existence
//        Project project = projectRepository.findById(id)
//                .orElseThrow(() -> new NotFoundException("Project not found"));
//
//        // ✅ FIX: Delete associated files first
//        List<FileMeta> projectFiles = fileMetaRepository.findByProjectId(id);
//        System.out.println("Deleting " + projectFiles.size() + " files for project: " + id);
//
//        for (FileMeta file : projectFiles) {
//            try {
//                fileService.deleteFile(file.getId(), deletedBy);
//            } catch (Exception e) {
//                System.err.println("Failed to delete file: " + file.getId() + ", error: " + e.getMessage());
//                // Continue with other files even if one fails
//            }
//        }
//
//        // ✅ Now delete the project
//        projectRepository.deleteById(id);
//        activityService.record(id, deletedBy, "PROJECT_DELETED", null);
//    }
//
//    @Override
//    public List<ProjectDto> listProjectsForEmployee(String employeeId, int page, int size) {
//        // TODO: implement pagination; naive implementation below
//        return projectRepository.findAll().stream()
//                .filter(p -> p.getAssignedEmployeeIds().contains(employeeId))
//                .map(ProjectMapper::toDto)
//                .map(this::enrich)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public void addAssignedEmployees(Long projectId, List<String> employeeIds, String actor) {
//        Project p = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
//        p.getAssignedEmployeeIds().addAll(employeeIds);
//        projectRepository.save(p);
//        // NEW: Send custom assignment notifications
//        Project project = projectRepository.findById(projectId).orElse(null);
//        if (project != null) {
//            sendProjectAssignmentNotifications(actor, employeeIds, project.getName(), projectId, "PROJECT ASSIGNED");
//        }
//        activityService.record(projectId, actor, "PROJECT_ASSIGNED_EMPLOYEES_ADDED", String.join(",", employeeIds));
//    }
//
//    @Override
//    @Transactional
//    public void removeAssignedEmployee(Long projectId, String employeeId, String actor) {
//        Project p = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
//        p.getAssignedEmployeeIds().remove(employeeId);
//        projectRepository.save(p);
//        activityService.record(projectId, actor, "PROJECT_ASSIGNED_EMPLOYEE_REMOVED", employeeId);
//    }
//
//    @Override
//    public void updateStatus(Long projectId, String status, String actor) {
//        Project p = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
//        p.setProjectStatus(Enum.valueOf(com.erp.project_service.entity.ProjectStatus.class, status));
//        projectRepository.save(p);
//
//        // NEW: Send project status change notification
//        sendProjectStatusChangeNotification(actor, p.getAssignedEmployeeIds(), p.getName(), status, projectId);
//
//        activityService.record(projectId, actor, "PROJECT_STATUS_UPDATED", status);
//    }
//
//    @Override
//    public void updateProgress(Long projectId, Integer percent, String actor) {
//        Project p = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
//        p.setProgressPercent(percent);
//        projectRepository.save(p);
//
//        // NEW: Send project progress update notification
//        sendProjectProgressNotification(actor, p.getAssignedEmployeeIds(), p.getName(), percent, projectId);
//
//        activityService.record(projectId, actor, "PROJECT_PROGRESS_UPDATED", percent.toString());
//    }
//
//    //Notification Helper Method
//    // NEW METHOD: Send project assignment notifications
//    private void sendProjectAssignmentNotifications(String actor, List<String> employeeIds, String projectName, Long projectId, String action) {
//        try {
//            String title = "";
//            String message = "";
//
//            if ("CREATED".equals(action)) {
//                title = "🚀 New Project Created";
//                message = String.format(
//                        "You have been assigned to a new project: '%s'. " +
//                                "Project ID: %d. " +
//                                "Welcome to the team! Please review the project details.",
//                        projectName, projectId
//                );
//            } else if ("UPDATED".equals(action)) {
//                title = "📋 Project Updated";
//                message = String.format(
//                        "Project '%s' has been updated. " +
//                                "Project ID: %d. " +
//                                "Please check the latest project information.",
//                        projectName, projectId
//                );
//            } else if ("ASSIGNED".equals(action)) {
//                title = "👥 Added to Project";
//                message = String.format(
//                        "You have been added to project: '%s'. " +
//                                "Project ID: %d. " +
//                                "You are now part of the project team.",
//                        projectName, projectId
//                );
//            }
//
//            notificationHelper.sendBulkNotification(actor, employeeIds, title, message, "PROJECT_ASSIGNMENT");
//
//            log.info("Project {} notifications sent to {} employees for project: {}", action, employeeIds.size(), projectId);
//
//        } catch (Exception e) {
//            log.error("Failed to send project {} notifications: {}", action, e.getMessage());
//        }
//    }
//
//    // NEW METHOD: Send project status change notification
//    private void sendProjectStatusChangeNotification(String actor, Set<String> assignedEmployeeIds, String projectName, String newStatus, Long projectId) {
//        try {
//            String title = "🔄 Project Status Updated";
//            String message = String.format(
//                    "Project '%s' status has been changed to: %s. " +
//                            "Project ID: %d. " +
//                            "This reflects the current state of the project.",
//                    projectName, newStatus, projectId
//            );
//
//            notificationHelper.sendBulkNotification(actor, new ArrayList<>(assignedEmployeeIds), title, message, "PROJECT_STATUS_CHANGE");
//
//            log.info("Project status change notification sent for project: {}", projectId);
//
//        } catch (Exception e) {
//            log.error("Failed to send project status change notification: {}", e.getMessage());
//        }
//    }
//
//    // NEW METHOD: Send project progress notification
//    private void sendProjectProgressNotification(String actor, Set<String> assignedEmployeeIds, String projectName, Integer progressPercent, Long projectId) {
//        try {
//            String title = "📊 Project Progress Updated";
//            String message = String.format(
//                    "Project '%s' progress has been updated to: %d%%. " +
//                            "Project ID: %d. " +
//                            "Keep up the great work!",
//                    projectName, progressPercent, projectId
//            );
//
//            notificationHelper.sendBulkNotification(actor, new ArrayList<>(assignedEmployeeIds), title, message, "PROJECT_PROGRESS");
//
//            log.info("Project progress notification sent for project: {}", projectId);
//
//        } catch (Exception e) {
//            log.error("Failed to send project progress notification: {}", e.getMessage());
//        }
//    }
//
//
//
//    @Override
//    public ProjectDto getProjectWithMetrics(Long projectId, String requesterId) {
//        ProjectDto dto = getProject(projectId, requesterId);
//
//        Long minutes = timeLogRepository.sumDurationMinutesByProjectId(projectId);
//        dto.setTotalTimeLoggedMinutes(minutes == null ? 0L : minutes);
//
//        java.math.BigDecimal expenses = projectMilestoneRepository.sumCostByProjectId(projectId);
//        dto.setExpenses(expenses == null ? java.math.BigDecimal.ZERO : expenses);
//
//        java.math.BigDecimal budget = dto.getBudget() == null ? java.math.BigDecimal.ZERO : dto.getBudget();
//        dto.setEarning(budget);
//        dto.setProfit(budget.subtract(dto.getExpenses() == null ? java.math.BigDecimal.ZERO : dto.getExpenses()));
//
//        return dto;
//    }
//
//
//    // helper to call clients and attach meta + files
//    private ProjectDto enrich(ProjectDto dto) {
//        if (dto == null) return null;
//
//        // client meta
//        try {
//            ClientMetaDto client = clientClient.getClients(dto.getClientId());
//            dto.setClient(client);
//        } catch (Exception ex) {
//            System.err.println("Failed to fetch client: " + dto.getClientId() + ", error: " + ex.getMessage());
//        }
//
//        // assigned employees meta
//        if (dto.getAssignedEmployeeIds() != null && !dto.getAssignedEmployeeIds().isEmpty()) {
//            List<EmployeeMetaDto> list = dto.getAssignedEmployeeIds().stream()
//                    .map(id -> {
//                        try { return employeeClient.getMeta(id); } catch (Exception e) { return null; }
//                    })
//                    .filter(x -> x != null).collect(Collectors.toList());
//            dto.setAssignedEmployees(list);
//        }
//
//        // ✅ FIX: Only get files for THIS specific project
//        if (dto.getId() != null) {
//            List<FileMetaDto> files = fileMetaRepository.findByProjectId(dto.getId()).stream()
//                    .map(FileMetaMapper::toDto)
//                    .collect(Collectors.toList());
//            dto.setCompanyFiles(files);
//        } else {
//            dto.setCompanyFiles(new ArrayList<>());
//        }
//
//        return dto;
//    }
//
//    //Project admin implementation
//    @Override
//    @Transactional
//    public void assignProjectAdmin(Long projectId, String userIdToMakeAdmin, String actor) {
//        Project p = projectRepository.findById(projectId)
//                .orElseThrow(() -> new NotFoundException("Project not found"));
//
//        // 1) ensure the user to be made admin is already assigned to the project
//        if (p.getAssignedEmployeeIds() == null || !p.getAssignedEmployeeIds().contains(userIdToMakeAdmin)) {
//            throw new IllegalArgumentException("User " + userIdToMakeAdmin + " is not assigned to project " + projectId);
//        }
//
//        String previousAdmin = p.getProjectAdminId();
//
//        // 2) if someone else is already admin, require explicit removal first
//        if (previousAdmin != null && !previousAdmin.equals(userIdToMakeAdmin)) {
//            throw new IllegalStateException("Project already has an admin (" + previousAdmin + "). Remove existing admin first before assigning a new one.");
//        }
//
//        // 3) set (idempotent if same user)
//        p.setProjectAdminId(userIdToMakeAdmin);
//        projectRepository.save(p);
//
//        activityService.record(projectId, actor, "PROJECT_ADMIN_ASSIGNED",
//                String.format("from=%s,to=%s", previousAdmin == null ? "NONE" : previousAdmin, userIdToMakeAdmin));
//
//        // notify new admin (best-effort)
//        try {
//            String title = "🛡️ Project Admin Assigned";
//            String message = String.format("You are now the admin for project '%s' (ID: %d).", p.getName(), projectId);
//            notificationHelper.sendBulkNotification(actor, List.of(userIdToMakeAdmin), title, message, "PROJECT_ADMIN_CHANGE");
//        } catch (Exception e) {
//            log.error("Failed to send project admin change notification: {}", e.getMessage());
//        }
//    }
//
//    @Override
//    @Transactional
//    public void removeProjectAdmin(Long projectId, String actor) {
//        Project p = projectRepository.findById(projectId)
//                .orElseThrow(() -> new NotFoundException("Project not found"));
//
//        String previousAdmin = p.getProjectAdminId();
//        if (previousAdmin == null) return; // nothing to do
//
//        p.setProjectAdminId(null);
//        projectRepository.save(p);
//
//        activityService.record(projectId, actor, "PROJECT_ADMIN_REMOVED", previousAdmin);
//
//        try {
//            String title = "🛑 Removed as Project Admin";
//            String message = String.format("You are no longer the admin for project '%s' (ID: %d).", p.getName(), projectId);
//            notificationHelper.sendBulkNotification(actor, List.of(previousAdmin), title, message, "PROJECT_ADMIN_CHANGE");
//        } catch (Exception e) {
//            log.error("Failed to send admin removal notification: {}", e.getMessage());
//        }
//    }
//
//
//    @Override
//    @Transactional(readOnly = true)
//    public List<ProjectDto> listProjectsByClient(String clientId) {
//        // find projects by clientId and enrich each dto
//        List<Project> projects = projectRepository.findByClientId(clientId);
//        return projects.stream()
//                .map(ProjectMapper::toDto)
//                .map(this::enrich)
//                .collect(Collectors.toList());
//    }
//
//    // inside ProjectServiceImpl class
//    @Override
//    @Transactional(readOnly = true)
//    public ClientProjectStatsDto getClientProjectStats(String clientId) {
//        if (clientId == null || clientId.trim().isEmpty()) {
//            return new ClientProjectStatsDto(0L, BigDecimal.ZERO);
//        }
//
//        // reuse existing repository method
//        List<Project> projects = projectRepository.findByClientId(clientId);
//
//        long count = projects == null ? 0L : projects.size();
//
//        BigDecimal total = projects == null
//                ? BigDecimal.ZERO
//                : projects.stream()
//                .map(p -> p.getBudget() == null ? BigDecimal.ZERO : p.getBudget())
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        return new ClientProjectStatsDto(count, total);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public EmployeeProjectCountDto getProjectCountForEmployee(String employeeId) {
//        if (employeeId == null || employeeId.trim().isEmpty()) {
//            return new EmployeeProjectCountDto(employeeId, 0L);
//        }
//
//        // Naive (but works) approach — reuses existing repository method patterns
//        long count = projectRepository.findAll().stream()
//                .filter(p -> p.getAssignedEmployeeIds() != null && p.getAssignedEmployeeIds().contains(employeeId))
//                .count();
//
//        return new EmployeeProjectCountDto(employeeId, count);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ProjectCountsDto getProjectCounts() {
//        // statuses considered pending
//        List<ProjectStatus> pendingStatuses = Arrays.asList(
//                ProjectStatus.IN_PROGRESS,
//                ProjectStatus.NOT_STARTED,
//                ProjectStatus.ON_HOLD
//        );
//
//        // Use Asia/Kolkata explicitly to match your timezone note
//        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
//
//        long pending = 0L;
//        long overdue = 0L;
//        try {
//            pending = projectRepository.countByProjectStatusIn(pendingStatuses);
//            overdue = projectRepository.countByProjectStatusInAndDeadlineBeforeAndNoDeadlineFalse(pendingStatuses, today);
//        } catch (Exception ex) {
//            log.warn("Failed to run optimized counts via repository methods, falling back to in-memory scan: {}", ex.getMessage());
//            // fallback - safe but less efficient
//            pending = projectRepository.findAll()
//                    .stream()
//                    .filter(p -> p.getProjectStatus() != null && pendingStatuses.contains(p.getProjectStatus()))
//                    .count();
//
//            overdue = projectRepository.findAll()
//                    .stream()
//                    .filter(p -> p.getProjectStatus() != null && pendingStatuses.contains(p.getProjectStatus()))
//                    .filter(p -> !Boolean.TRUE.equals(p.isNoDeadline()))
//                    .filter(p -> p.getDeadline() != null && p.getDeadline().isBefore(today))
//                    .count();
//        }
//
//        return new ProjectCountsDto(pending, overdue);
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public ProjectCountsDto getProjectCountsForEmployee(String employeeId) {
//        if (employeeId == null || employeeId.trim().isEmpty()) {
//            return new ProjectCountsDto(0L, 0L);
//        }
//
//        List<ProjectStatus> pendingStatuses = Arrays.asList(
//                ProjectStatus.IN_PROGRESS,
//                ProjectStatus.NOT_STARTED,
//                ProjectStatus.ON_HOLD
//        );
//
//        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
//
//        long pending = 0L;
//        long overdue = 0L;
//        try {
//            pending = projectRepository.countByEmployeeAndStatuses(employeeId, pendingStatuses);
//            overdue = projectRepository.countOverdueByEmployeeAndStatuses(employeeId, pendingStatuses, today);
//        } catch (Exception ex) {
//            log.warn("Repo optimized counts failed for employee {}, falling back to in-memory scan: {}", employeeId, ex.getMessage());
//            // fallback safe approach
//            pending = projectRepository.findAll()
//                    .stream()
//                    .filter(p -> p.getAssignedEmployeeIds() != null && p.getAssignedEmployeeIds().contains(employeeId))
//                    .filter(p -> p.getProjectStatus() != null && pendingStatuses.contains(p.getProjectStatus()))
//                    .count();
//
//            overdue = projectRepository.findAll()
//                    .stream()
//                    .filter(p -> p.getAssignedEmployeeIds() != null && p.getAssignedEmployeeIds().contains(employeeId))
//                    .filter(p -> p.getProjectStatus() != null && pendingStatuses.contains(p.getProjectStatus()))
//                    .filter(p -> !Boolean.TRUE.equals(p.isNoDeadline()))
//                    .filter(p -> p.getDeadline() != null && p.getDeadline().isBefore(today))
//                    .count();
//        }
//
//        return new ProjectCountsDto(pending, overdue);
//    }
//
//}

package com.erp.project_service.service.impl;

import com.erp.project_service.client.ClientClient;
import com.erp.project_service.client.EmployeeClient;
import com.erp.project_service.dto.common.ClientMetaDto;
import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.dto.file.FileMetaDto;
import com.erp.project_service.dto.project.*;
        import com.erp.project_service.entity.FileMeta;
import com.erp.project_service.entity.Project;
import com.erp.project_service.entity.ProjectStatus;
import com.erp.project_service.exception.DuplicateResourceException;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.FileMetaMapper;
import com.erp.project_service.mapper.ProjectMapper;
import com.erp.project_service.repository.FileMetaRepository;
import com.erp.project_service.repository.ProjectMilestoneRepository;
import com.erp.project_service.repository.ProjectRepository;
import com.erp.project_service.repository.TimeLogRepository;
import com.erp.project_service.service.interfaces.FileService;
import com.erp.project_service.service.interfaces.ProjectService;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import com.erp.project_service.service.notification.NotificationHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final FileMetaRepository fileMetaRepository;
    private final EmployeeClient employeeClient;
    private final ClientClient clientClient;
    private final ProjectActivityService activityService;
    private final TimeLogRepository timeLogRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final FileService fileService;
    private final NotificationHelperService notificationHelper; // NEW

    @Override
    @Transactional
    public ProjectDto createProject(ProjectCreateRequest req, String createdBy) {
        if (req.getShortCode() != null && projectRepository.existsByShortCode(req.getShortCode())) {
            throw new DuplicateResourceException("Project shortCode '" + req.getShortCode() + "' already exists");
        }
        // Create project first
        Project p = ProjectMapper.toEntity(req);
        p.setCreatedBy(createdBy);
        p.setAddedBy(createdBy);
        Project saved = projectRepository.save(p);

        // Handle file upload if present
        if (req.getCompanyFile() != null && !req.getCompanyFile().isEmpty()) {
            uploadCompanyFile(req.getCompanyFile(), saved.getId(), createdBy);
        }

        // NEW: Send custom project creation notifications
        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
            System.out.println("DEBUG: Type of assignedEmployeeIds: " + req.getAssignedEmployeeIds().getClass().getName());
            List<String> employeeIds = new ArrayList<>(req.getAssignedEmployeeIds());
            System.out.println("DEBUG: After conversion to List: " + employeeIds.getClass().getName());
            sendProjectAssignmentNotifications(createdBy, employeeIds, req.getName(), saved.getId(), "PROJECT CREATED");
        }

        activityService.record(saved.getId(), createdBy, "PROJECT_CREATED", null);
        return enrich(ProjectMapper.toDto(saved));
    }

    private void uploadCompanyFile(MultipartFile companyFile, Long projectId, String uploadedBy) {
        try {
            // Use existing file service to upload
            fileService.uploadProjectFile(projectId, companyFile, uploadedBy);
        } catch (Exception e) {
            // Log error but don't fail project creation
            System.err.println("Failed to upload company file: " + e.getMessage());
        }
    }

    @Override
    public ProjectDto getProject(Long id, String requesterId) {
        Project p = projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));
        return enrich(ProjectMapper.toDto(p));
    }

    @Override
    @Transactional
    public ProjectDto updateProject(Long id, ProjectUpdateRequest req, String updatedBy) {
        Project p = projectRepository.findById(id).orElseThrow(() -> new NotFoundException("Project not found"));

        // Apply all updates using ProjectMapper
        ProjectMapper.applyUpdate(req, p);
        p.setUpdatedBy(updatedBy);

        Project saved = projectRepository.save(p);

        // Handle file upload if present during update
        if (req.getCompanyFile() != null && !req.getCompanyFile().isEmpty()) {
            uploadCompanyFile(req.getCompanyFile(), saved.getId(), updatedBy);
        }

        if (req.getAssignedEmployeeIds() != null && !req.getAssignedEmployeeIds().isEmpty()) {
            List<String> employeeIds = new ArrayList<>(req.getAssignedEmployeeIds());
            sendProjectAssignmentNotifications(updatedBy, employeeIds, req.getName(), saved.getId(), "PROJECT CREATED");
        }

        activityService.record(saved.getId(), updatedBy, "PROJECT_UPDATED", null);
        return enrich(ProjectMapper.toDto(saved));
    }

    @Transactional
    @Override
    public List<ProjectDto> getAll(){
        return projectRepository.findAll()
                .stream()
                .map(ProjectMapper::toDto)
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProject(Long id, String deletedBy) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        List<FileMeta> projectFiles = fileMetaRepository.findByProjectId(id);
        System.out.println("Deleting " + projectFiles.size() + " files for project: " + id);

        for (FileMeta file : projectFiles) {
            try {
                fileService.deleteFile(file.getId(), deletedBy);
            } catch (Exception e) {
                System.err.println("Failed to delete file: " + file.getId() + ", error: " + e.getMessage());
            }
        }

        projectRepository.deleteById(id);
        activityService.record(id, deletedBy, "PROJECT_DELETED", null);
    }

    @Override
    public List<ProjectDto> listProjectsForEmployee(String employeeId, int page, int size) {
        return projectRepository.findAll().stream()
                .filter(p -> p.getAssignedEmployeeIds().contains(employeeId))
                .map(ProjectMapper::toDto)
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addAssignedEmployees(Long projectId, List<String> employeeIds, String actor) {
        Project p = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        p.getAssignedEmployeeIds().addAll(employeeIds);
        projectRepository.save(p);
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null) {
            sendProjectAssignmentNotifications(actor, employeeIds, project.getName(), projectId, "PROJECT ASSIGNED");
        }
        activityService.record(projectId, actor, "PROJECT_ASSIGNED_EMPLOYEES_ADDED", String.join(",", employeeIds));
    }

    @Override
    @Transactional
    public void removeAssignedEmployee(Long projectId, String employeeId, String actor) {
        Project p = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        p.getAssignedEmployeeIds().remove(employeeId);
        projectRepository.save(p);
        activityService.record(projectId, actor, "PROJECT_ASSIGNED_EMPLOYEE_REMOVED", employeeId);
    }

    @Override
    public void updateStatus(Long projectId, String status, String actor) {
        Project p = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        p.setProjectStatus(Enum.valueOf(com.erp.project_service.entity.ProjectStatus.class, status));
        projectRepository.save(p);

        sendProjectStatusChangeNotification(actor, p.getAssignedEmployeeIds(), p.getName(), status, projectId);

        activityService.record(projectId, actor, "PROJECT_STATUS_UPDATED", status);
    }

    @Override
    public void updateProgress(Long projectId, Integer percent, String actor) {
        Project p = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        p.setProgressPercent(percent);
        projectRepository.save(p);

        sendProjectProgressNotification(actor, p.getAssignedEmployeeIds(), p.getName(), percent, projectId);

        activityService.record(projectId, actor, "PROJECT_PROGRESS_UPDATED", percent.toString());
    }

    // safe helpers to avoid external call crashes
    private EmployeeMetaDto safeGetEmployeeMeta(String employeeId) {
        if (employeeId == null) return null;
        try {
            return employeeClient.getMeta(employeeId);
        } catch (Exception ex) {
            log.debug("safeGetEmployeeMeta: failed for {} -> {}", employeeId, ex.getMessage());
            return null;
        }
    }

    private ClientMetaDto safeGetClientMeta(String clientId) {
        if (clientId == null) return null;
        try {
            // the clientClient has two methods; use the string one to be safe
            return clientClient.getClients(clientId);
        } catch (Exception ex) {
            log.debug("safeGetClientMeta: failed for {} -> {}", clientId, ex.getMessage());
            return null;
        }
    }

    //Notification Helper Method
    private void sendProjectAssignmentNotifications(String actor, List<String> employeeIds, String projectName, Long projectId, String action) {
        try {
            String title = "";
            String message = "";

            if ("CREATED".equals(action)) {
                title = "🚀 New Project Created";
                message = String.format(
                        "You have been assigned to a new project: '%s'. Project ID: %d. Welcome to the team! Please review the project details.",
                        projectName, projectId
                );
            } else if ("UPDATED".equals(action)) {
                title = "📋 Project Updated";
                message = String.format(
                        "Project '%s' has been updated. Project ID: %d. Please check the latest project information.",
                        projectName, projectId
                );
            } else if ("ASSIGNED".equals(action)) {
                title = "👥 Added to Project";
                message = String.format(
                        "You have been added to project: '%s'. Project ID: %d. You are now part of the project team.",
                        projectName, projectId
                );
            }

            notificationHelper.sendBulkNotification(actor, employeeIds, title, message, "PROJECT_ASSIGNMENT");

            log.info("Project {} notifications sent to {} employees for project: {}", action, employeeIds.size(), projectId);

        } catch (Exception e) {
            log.error("Failed to send project {} notifications: {}", action, e.getMessage());
        }
    }

    private void sendProjectStatusChangeNotification(String actor, Set<String> assignedEmployeeIds, String projectName, String newStatus, Long projectId) {
        try {
            String title = "🔄 Project Status Updated";
            String message = String.format(
                    "Project '%s' status has been changed to: %s. Project ID: %d.",
                    projectName, newStatus, projectId
            );

            notificationHelper.sendBulkNotification(actor, new ArrayList<>(assignedEmployeeIds), title, message, "PROJECT_STATUS_CHANGE");

            log.info("Project status change notification sent for project: {}", projectId);

        } catch (Exception e) {
            log.error("Failed to send project status change notification: {}", e.getMessage());
        }
    }

    private void sendProjectProgressNotification(String actor, Set<String> assignedEmployeeIds, String projectName, Integer progressPercent, Long projectId) {
        try {
            String title = "📊 Project Progress Updated";
            String message = String.format(
                    "Project '%s' progress has been updated to: %d%%. Project ID: %d.",
                    projectName, progressPercent, projectId
            );

            notificationHelper.sendBulkNotification(actor, new ArrayList<>(assignedEmployeeIds), title, message, "PROJECT_PROGRESS");

            log.info("Project progress notification sent for project: {}", projectId);

        } catch (Exception e) {
            log.error("Failed to send project progress notification: {}", e.getMessage());
        }
    }

    @Override
    public ProjectDto getProjectWithMetrics(Long projectId, String requesterId) {
        ProjectDto dto = getProject(projectId, requesterId);

        Long minutes = timeLogRepository.sumDurationMinutesByProjectId(projectId);
        dto.setTotalTimeLoggedMinutes(minutes == null ? 0L : minutes);

        java.math.BigDecimal expenses = projectMilestoneRepository.sumCostByProjectId(projectId);
        dto.setExpenses(expenses == null ? java.math.BigDecimal.ZERO : expenses);

        java.math.BigDecimal budget = dto.getBudget() == null ? java.math.BigDecimal.ZERO : dto.getBudget();
        dto.setEarning(budget);
        dto.setProfit(budget.subtract(dto.getExpenses() == null ? java.math.BigDecimal.ZERO : dto.getExpenses()));

        return dto;
    }

    //Project admin implementation
    @Override
    @Transactional
    public void assignProjectAdmin(Long projectId, String userIdToMakeAdmin, String actor) {
        Project p = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        // 1) ensure the user to be made admin is already assigned to the project
        if (p.getAssignedEmployeeIds() == null || !p.getAssignedEmployeeIds().contains(userIdToMakeAdmin)) {
            throw new IllegalArgumentException("User " + userIdToMakeAdmin + " is not assigned to project " + projectId);
        }

        String previousAdmin = p.getProjectAdminId();

        // 2) if someone else is already admin, require explicit removal first
        if (previousAdmin != null && !previousAdmin.equals(userIdToMakeAdmin)) {
            throw new IllegalStateException("Project already has an admin (" + previousAdmin + "). Remove existing admin first before assigning a new one.");
        }

        // 3) set (idempotent if same user)
        p.setProjectAdminId(userIdToMakeAdmin);
        projectRepository.save(p);

        activityService.record(projectId, actor, "PROJECT_ADMIN_ASSIGNED",
                String.format("from=%s,to=%s", previousAdmin == null ? "NONE" : previousAdmin, userIdToMakeAdmin));

        // notify new admin (best-effort)
        try {
            String title = "🛡️ Project Admin Assigned";
            String message = String.format("You are now the admin for project '%s' (ID: %d).", p.getName(), projectId);
            notificationHelper.sendBulkNotification(actor, List.of(userIdToMakeAdmin), title, message, "PROJECT_ADMIN_CHANGE");
        } catch (Exception e) {
            log.error("Failed to send project admin change notification: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void removeProjectAdmin(Long projectId, String actor) {
        Project p = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        String previousAdmin = p.getProjectAdminId();
        if (previousAdmin == null) return; // nothing to do

        p.setProjectAdminId(null);
        projectRepository.save(p);

        activityService.record(projectId, actor, "PROJECT_ADMIN_REMOVED", previousAdmin);

        try {
            String title = "🛑 Removed as Project Admin";
            String message = String.format("You are no longer the admin for project '%s' (ID: %d).", p.getName(), projectId);
            notificationHelper.sendBulkNotification(actor, List.of(previousAdmin), title, message, "PROJECT_ADMIN_CHANGE");
        } catch (Exception e) {
            log.error("Failed to send admin removal notification: {}", e.getMessage());
        }
    }

    // helper to call clients and attach meta + files (defensive)
    private ProjectDto enrich(ProjectDto dto) {
        if (dto == null) return null;

        // client meta (safe)
        try {
            ClientMetaDto client = safeGetClientMeta(dto.getClientId());
            dto.setClient(client);
        } catch (Exception ex) {
            log.debug("enrich: client meta fetch unexpected error for {} -> {}", dto.getClientId(), ex.getMessage());
        }

        // assigned employees meta (safe)
        if (dto.getAssignedEmployeeIds() != null && !dto.getAssignedEmployeeIds().isEmpty()) {
            List<EmployeeMetaDto> list = new ArrayList<>();
            for (String id : dto.getAssignedEmployeeIds()) {
                EmployeeMetaDto em = safeGetEmployeeMeta(id);
                if (em != null) list.add(em);
            }
            dto.setAssignedEmployees(list);
        } else {
            dto.setAssignedEmployees(new ArrayList<>());
        }

        // files for this project
        if (dto.getId() != null) {
            try {
                List<FileMetaDto> files = fileMetaRepository.findByProjectId(dto.getId()).stream()
                        .map(FileMetaMapper::toDto)
                        .collect(Collectors.toList());
                dto.setCompanyFiles(files);
            } catch (Exception ex) {
                log.warn("Failed to fetch files for project {} : {}", dto.getId(), ex.getMessage());
                dto.setCompanyFiles(new ArrayList<>());
            }
        } else {
            dto.setCompanyFiles(new ArrayList<>());
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDto> listProjectsByClient(String clientId) {
        List<Project> projects = projectRepository.findByClientId(clientId);
        return projects.stream()
                .map(ProjectMapper::toDto)
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientProjectStatsDto getClientProjectStats(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            return new ClientProjectStatsDto(0L, BigDecimal.ZERO);
        }

        List<Project> projects = projectRepository.findByClientId(clientId);

        long count = projects == null ? 0L : projects.size();

        BigDecimal total = projects == null
                ? BigDecimal.ZERO
                : projects.stream()
                .map(p -> p.getBudget() == null ? BigDecimal.ZERO : p.getBudget())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClientProjectStatsDto(count, total);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeProjectCountDto getProjectCountForEmployee(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return new EmployeeProjectCountDto(employeeId, 0L);
        }

        long count = projectRepository.findAll().stream()
                .filter(p -> p.getAssignedEmployeeIds() != null && p.getAssignedEmployeeIds().contains(employeeId))
                .count();

        return new EmployeeProjectCountDto(employeeId, count);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectCountsDto getProjectCounts() {
        List<ProjectStatus> pendingStatuses = Arrays.asList(
                ProjectStatus.IN_PROGRESS,
                ProjectStatus.NOT_STARTED,
                ProjectStatus.ON_HOLD
        );

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        long pending = 0L;
        long overdue = 0L;
        try {
            pending = projectRepository.countByProjectStatusIn(pendingStatuses);
            overdue = projectRepository.countByProjectStatusInAndDeadlineBeforeAndNoDeadlineFalse(pendingStatuses, today);
        } catch (Exception ex) {
            log.warn("Failed to run optimized counts via repository methods, falling back to in-memory scan: {}", ex.getMessage());
            pending = projectRepository.findAll()
                    .stream()
                    .filter(p -> p.getProjectStatus() != null && pendingStatuses.contains(p.getProjectStatus()))
                    .count();

            overdue = projectRepository.findAll()
                    .stream()
                    .filter(p -> p.getProjectStatus() != null && pendingStatuses.contains(p.getProjectStatus()))
                    .filter(p -> !Boolean.TRUE.equals(p.isNoDeadline()))
                    .filter(p -> p.getDeadline() != null && p.getDeadline().isBefore(today))
                    .count();
        }

        return new ProjectCountsDto(pending, overdue);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectCountsDto getProjectCountsForEmployee(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return new ProjectCountsDto(0L, 0L);
        }

        List<ProjectStatus> pendingStatuses = Arrays.asList(
                ProjectStatus.IN_PROGRESS,
                ProjectStatus.NOT_STARTED,
                ProjectStatus.ON_HOLD
        );

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        long pending = 0L;
        long overdue = 0L;
        try {
            pending = projectRepository.countByEmployeeAndStatuses(employeeId, pendingStatuses);
            overdue = projectRepository.countOverdueByEmployeeAndStatuses(employeeId, pendingStatuses, today);
        } catch (Exception ex) {
            log.warn("Repo optimized counts failed for employee {}, falling back to in-memory scan: {}", employeeId, ex.getMessage());
            pending = projectRepository.findAll()
                    .stream()
                    .filter(p -> p.getAssignedEmployeeIds() != null && p.getAssignedEmployeeIds().contains(employeeId))
                    .filter(p -> p.getProjectStatus() != null && pendingStatuses.contains(p.getProjectStatus()))
                    .count();

            overdue = projectRepository.findAll()
                    .stream()
                    .filter(p -> p.getAssignedEmployeeIds() != null && p.getAssignedEmployeeIds().contains(employeeId))
                    .filter(p -> p.getProjectStatus() != null && pendingStatuses.contains(p.getProjectStatus()))
                    .filter(p -> !Boolean.TRUE.equals(p.isNoDeadline()))
                    .filter(p -> p.getDeadline() != null && p.getDeadline().isBefore(today))
                    .count();
        }

        return new ProjectCountsDto(pending, overdue);
    }

    @Override
    public List<ProjectDto> listProjectsForEmployees(String userId) {
        return projectRepository.findAll().stream()
                .filter(p -> p.getAssignedEmployeeIds().contains(userId))
                .map(ProjectMapper::toDto)
                .map(this::enrich)
                .collect(Collectors.toList());
    }

}
