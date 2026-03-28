package com.erp.employee_service.service.leave;

import com.erp.employee_service.dto.leave.*;
import com.erp.employee_service.dto.notification.SendNotificationDto;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.entity.FileMeta;
import com.erp.employee_service.entity.leave.*;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.repository.LeaveRepository;
import com.erp.employee_service.repository.LeaveQuotaRepository;
import com.erp.employee_service.repository.FileMetaRepository;
import com.erp.employee_service.service.SupabaseStorageService;
import com.erp.employee_service.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveQuotaRepository leaveQuotaRepository;
    private final NotificationService notificationService;
    private final SupabaseStorageService supabaseStorageService;
    private final FileMetaRepository fileMetaService;

    public LeaveResponseDto applyLeave(String employeeId, LeaveRequestDto requestDto, List<MultipartFile> documents) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        validateLeaveRequest(requestDto);
        int leaveDays = calculateLeaveDays(requestDto);

        // Check leave quota and determine if paid
        boolean isPaid = isLeaveOverutilized(employee, requestDto.getLeaveType().name(), leaveDays);

        Leave leave = createLeaveFromRequest(employee, requestDto);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setIsPaid(isPaid); // Set isPaid flag

        // Handle file uploads
        if (documents != null && !documents.isEmpty()) {
            List<FileMeta> uploadedFiles = uploadLeaveDocuments(documents, employee, "LEAVE_DOCUMENT");
            leave.setDocuments(uploadedFiles);
        }

        Leave savedLeave = leaveRepository.save(leave);

        // Notify admins about new leave application
        notifyAdminsAboutNewLeave(employee, savedLeave);

        // Send notification to employee
        sendLeaveApplicationNotification(employee, savedLeave);

        return convertToDto(savedLeave);
    }

    public List<LeaveResponseDto> applyLeavesForEmployees(AdminLeaveRequestDto requestDto, List<MultipartFile> documents, String adminId) {
        Employee admin = employeeRepository.findByEmployeeId(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        validateAdminLeaveRequest(requestDto);

        // Handle file uploads once for all employees
        List<FileMeta> uploadedFiles;
        if (documents != null && !documents.isEmpty()) {
            uploadedFiles = uploadLeaveDocuments(documents, admin, "LEAVE_DOCUMENT");
        } else {
            uploadedFiles = new ArrayList<>();
        }

        return requestDto.getEmployeeIds().stream()
                .map(empId -> {
                    Employee employee = employeeRepository.findByEmployeeId(empId)
                            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + empId));

                    int leaveDays = calculateAdminLeaveDays(requestDto);

                    Leave leave = createLeaveFromAdminRequest(employee, requestDto);
                    leave.setStatus(requestDto.getStatus());

                    // Check leave quota and determine if paid if status is approved
                    boolean isPaid = false;
                    if (requestDto.getStatus() == LeaveStatus.APPROVED) {
                        isPaid = isLeaveOverutilized(employee, requestDto.getLeaveType().name(), leaveDays);
                        leave.setIsPaid(isPaid); // Set isPaid flag

                        // Handle quota deduction based on isPaid status
                        if (isPaid) {
                            // Overutilized leave - add to overutilized count
                            addToOverUtilized(employee, requestDto.getLeaveType().name(), leaveDays);
                        } else {
                            // Within quota - deduct from regular quota
                            deductFromLeaveQuota(employee, requestDto.getLeaveType().name(), leaveDays);
                        }
                    }

                    // Create new FileMeta instances for each employee to avoid shared references
                    List<FileMeta> employeeDocuments = new ArrayList<>();
                    for (FileMeta fileMeta : uploadedFiles) {
                        FileMeta newFileMeta = new FileMeta();
                        newFileMeta.setBucket(fileMeta.getBucket());
                        newFileMeta.setPath(fileMeta.getPath());
                        newFileMeta.setFilename(fileMeta.getFilename());
                        newFileMeta.setMime(fileMeta.getMime());
                        newFileMeta.setSize(fileMeta.getSize());
                        newFileMeta.setUrl(fileMeta.getUrl());
                        newFileMeta.setUploadedBy(fileMeta.getUploadedBy());
                        newFileMeta.setEmployee(employee);
                        newFileMeta.setEntityType(fileMeta.getEntityType());
                        newFileMeta.setUploadedAt(fileMeta.getUploadedAt());
                        employeeDocuments.add(fileMetaService.save(newFileMeta));
                    }

                    leave.setDocuments(employeeDocuments);

                    if (requestDto.getStatus() == LeaveStatus.APPROVED) {
                        leave.setApprovedBy(admin);
                        leave.setApprovedAt(LocalDateTime.now());
                    }

                    Leave savedLeave = leaveRepository.save(leave);

                    // Notify employee about leave creation/approval
                    notifyEmployeeAboutLeave(employee, savedLeave, admin);

                    return convertToDto(savedLeave);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getMyLeaves(String employeeId) {
        return leaveRepository.findByEmployeeEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getAllLeaves() {
        return leaveRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getPendingLeaves() {
        return leaveRepository.findByStatusOrderByCreatedAtDesc(LeaveStatus.PENDING)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public LeaveResponseDto updateLeaveStatus(Long leaveId, LeaveStatusUpdateDto statusDto, String adminId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        Employee admin = employeeRepository.findByEmployeeId(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        if (statusDto.getStatus() == LeaveStatus.REJECTED &&
                (statusDto.getRejectionReason() == null || statusDto.getRejectionReason().trim().isEmpty())) {
            throw new IllegalArgumentException("Rejection reason is required when rejecting leave");
        }

        LeaveStatus oldStatus = leave.getStatus();
        LeaveStatus newStatus = statusDto.getStatus();

        log.info("Updating leave {} status from {} to {}", leaveId, oldStatus, newStatus);

        leave.setStatus(newStatus);

        // Handle quota updates based on status change
        handleQuotaUpdateForStatusChange(leave, oldStatus, newStatus);

        if (newStatus == LeaveStatus.APPROVED) {
            leave.setApprovedBy(admin);
            leave.setApprovedAt(LocalDateTime.now());
            leave.setRejectionReason(null);
            leave.setRejectedAt(null);
        } else if (newStatus == LeaveStatus.REJECTED) {
            leave.setRejectionReason(statusDto.getRejectionReason());
            leave.setRejectedAt(LocalDateTime.now());
            leave.setApprovedBy(null);
            leave.setApprovedAt(null);
        } else if (newStatus == LeaveStatus.PENDING) {
            // Reset approval/rejection fields if status goes back to pending
            leave.setApprovedBy(null);
            leave.setApprovedAt(null);
            leave.setRejectionReason(null);
            leave.setRejectedAt(null);
        }

        Leave updatedLeave = leaveRepository.save(leave);

        log.info("Leave {} status successfully updated to {}", leaveId, updatedLeave.getStatus());
        log.info("IsPaid flag set to: {}", updatedLeave.getIsPaid());

        // Notify employee about status change
        notifyEmployeeAboutStatusChange(leave.getEmployee(), updatedLeave, admin);

        return convertToDto(updatedLeave);
    }

    public LeaveResponseDto updateLeave(Long leaveId, LeaveRequestDto requestDto, List<MultipartFile> newDocuments, String updaterId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        Employee updater = employeeRepository.findByEmployeeId(updaterId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Check permissions
        if (!updater.isAdmin() && !leave.getEmployee().getEmployeeId().equals(updaterId)) {
            throw new SecurityException("Not authorized to update this leave");
        }

        validateLeaveRequest(requestDto);

        // Handle quota changes if status was approved
        if (leave.getStatus() == LeaveStatus.APPROVED) {
            int oldDays = calculateLeaveDays(leave);
            int newDays = calculateLeaveDays(requestDto);

            if (oldDays != newDays || !leave.getLeaveType().equals(requestDto.getLeaveType())) {
                // Restore old quota based on previous isPaid status
                if (leave.getIsPaid()) {
                    // Was overutilized - restore from overutilized
                    restoreFromOverUtilized(leave.getEmployee(), leave.getLeaveType().name(), oldDays);
                } else {
                    // Was within quota - restore to regular quota
                    restoreToLeaveQuota(leave.getEmployee(), leave.getLeaveType().name(), oldDays);
                }

                // Check and handle new quota, determine if paid
                boolean isPaid = isLeaveOverutilized(leave.getEmployee(), requestDto.getLeaveType().name(), newDays);
                leave.setIsPaid(isPaid);

                if (isPaid) {
                    // New leave is overutilized - add to overutilized
                    addToOverUtilized(leave.getEmployee(), requestDto.getLeaveType().name(), newDays);
                } else {
                    // New leave is within quota - deduct from regular quota
                    deductFromLeaveQuota(leave.getEmployee(), requestDto.getLeaveType().name(), newDays);
                }
            }
        }

        // Handle new document uploads
        if (newDocuments != null && !newDocuments.isEmpty()) {
            List<FileMeta> uploadedFiles = uploadLeaveDocuments(newDocuments, leave.getEmployee(), "LEAVE_DOCUMENT");
            leave.getDocuments().addAll(uploadedFiles);
        }

        // Update leave fields
        leave.setLeaveType(requestDto.getLeaveType());
        leave.setDurationType(requestDto.getDurationType());
        leave.setStartDate(requestDto.getStartDate());
        leave.setEndDate(requestDto.getEndDate());
        leave.setSingleDate(requestDto.getSingleDate());
        leave.setReason(requestDto.getReason());
        leave.setUpdatedAt(LocalDateTime.now());

        Leave updatedLeave = leaveRepository.save(leave);

        // Send notification about leave update
        sendLeaveUpdateNotification(leave.getEmployee(), updatedLeave, updater);

        return convertToDto(updatedLeave);
    }

    @Transactional(readOnly = true)
    public List<LeaveCalendarDto> getLeaveCalendar(LocalDate date) {
        List<Leave> approvedLeaves = leaveRepository.findApprovedLeavesByDate(date);

        return approvedLeaves.stream()
                .collect(Collectors.groupingBy(leave -> {
                    if (leave.getDurationType() == DurationType.MULTIPLE) {
                        return (date.isAfter(leave.getStartDate()) && date.isBefore(leave.getEndDate())) ?
                                date : leave.getStartDate();
                    } else {
                        return leave.getSingleDate();
                    }
                }))
                .entrySet()
                .stream()
                .map(entry -> {
                    LeaveCalendarDto calendarDto = new LeaveCalendarDto();
                    calendarDto.setDate(entry.getKey());
                    calendarDto.setEmployeesOnLeave(entry.getValue().stream()
                            .map(this::convertToEmployeeOnLeaveDto)
                            .collect(Collectors.toList()));
                    return calendarDto;
                })
                .collect(Collectors.toList());
    }

    public void deleteLeave(Long leaveId, String employeeId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        // Only allow deletion if user owns the leave or is admin
        if (!leave.getEmployee().getEmployeeId().equals(employeeId) && !employee.isAdmin()) {
            throw new SecurityException("Not authorized to delete this leave");
        }

        // Delete associated documents
        deleteLeaveDocuments(leave.getDocuments());

        // Restore quota if leave was approved
        if (leave.getStatus() == LeaveStatus.APPROVED) {
            int leaveDays = calculateLeaveDays(leave);
            if (leave.getIsPaid()) {
                // Was overutilized - restore from overutilized
                restoreFromOverUtilized(leave.getEmployee(), leave.getLeaveType().name(), leaveDays);
            } else {
                // Was within quota - restore to regular quota
                restoreToLeaveQuota(leave.getEmployee(), leave.getLeaveType().name(), leaveDays);
            }
        }

        // Send notification before deletion
        sendLeaveDeletionNotification(leave.getEmployee(), leave);

        leaveRepository.delete(leave);
    }

    public void deleteLeaveDocument(Long leaveId, Long documentId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave not found"));

        FileMeta document = leave.getDocuments().stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        // Remove from leave documents
        leave.getDocuments().remove(document);
        leaveRepository.save(leave);

        // Delete from storage and database
        supabaseStorageService.deleteFile(document.getPath());
        fileMetaService.deleteById(document.getId());

        // Send notification about document deletion
        sendLeaveDocumentDeletionNotification(leave.getEmployee(), leave, document);
    }

    // ========== NOTIFICATION METHODS ==========

    private void sendLeaveApplicationNotification(Employee employee, Leave leave) {
        try {
            String title = "Leave Application Submitted";
            String message = String.format("Your %s leave application for %s has been submitted successfully.",
                    leave.getLeaveType().name(),
                    leave.getDurationType() == DurationType.MULTIPLE ?
                            leave.getStartDate() + " to " + leave.getEndDate() :
                            leave.getSingleDate());

            SendNotificationDto notificationDto = new SendNotificationDto();
            notificationDto.setReceiverEmployeeId(employee.getEmployeeId());
            notificationDto.setTitle(title);
            notificationDto.setMessage(message);
            notificationDto.setType("LEAVE_APPLICATION");

            notificationService.sendNotification(
                    employee.getEmployeeId(),
                    notificationDto
            );
        } catch (Exception e) {
            log.error("Failed to send leave application notification to employee {}: {}",
                    employee.getEmployeeId(), e.getMessage());
        }
    }

    private void sendLeaveUpdateNotification(Employee employee, Leave leave, Employee updater) {
        try {
            String title = "Leave Application Updated";
            String message = String.format("Your %s leave application has been updated by %s.",
                    leave.getLeaveType().name(),
                    updater.getName());

            SendNotificationDto notificationDto = new SendNotificationDto();
            notificationDto.setReceiverEmployeeId(employee.getEmployeeId());
            notificationDto.setTitle(title);
            notificationDto.setMessage(message);
            notificationDto.setType("LEAVE_UPDATE");

            notificationService.sendNotification(
                    updater.getEmployeeId(),
                    notificationDto
            );
        } catch (Exception e) {
            log.error("Failed to send leave update notification to employee {}: {}",
                    employee.getEmployeeId(), e.getMessage());
        }
    }

    private void sendLeaveDeletionNotification(Employee employee, Leave leave) {
        try {
            String title = "Leave Application Deleted";
            String message = String.format("Your %s leave application for %s has been deleted.",
                    leave.getLeaveType().name(),
                    leave.getDurationType() == DurationType.MULTIPLE ?
                            leave.getStartDate() + " to " + leave.getEndDate() :
                            leave.getSingleDate());

            SendNotificationDto notificationDto = new SendNotificationDto();
            notificationDto.setReceiverEmployeeId(employee.getEmployeeId());
            notificationDto.setTitle(title);
            notificationDto.setMessage(message);
            notificationDto.setType("LEAVE_DELETION");

            notificationService.sendNotification(
                    null, // system generated
                    notificationDto
            );
        } catch (Exception e) {
            log.error("Failed to send leave deletion notification to employee {}: {}",
                    employee.getEmployeeId(), e.getMessage());
        }
    }

    private void sendLeaveDocumentDeletionNotification(Employee employee, Leave leave, FileMeta document) {
        try {
            String title = "Leave Document Removed";
            String message = String.format("A document has been removed from your %s leave application.",
                    leave.getLeaveType().name());

            SendNotificationDto notificationDto = new SendNotificationDto();
            notificationDto.setReceiverEmployeeId(employee.getEmployeeId());
            notificationDto.setTitle(title);
            notificationDto.setMessage(message);
            notificationDto.setType("LEAVE_DOCUMENT_DELETION");

            notificationService.sendNotification(
                    null, // system generated
                    notificationDto
            );
        } catch (Exception e) {
            log.error("Failed to send leave document deletion notification to employee {}: {}",
                    employee.getEmployeeId(), e.getMessage());
        }
    }

    // ========== EXISTING HELPER METHODS (unchanged) ==========

    private void handleQuotaUpdateForStatusChange(Leave leave, LeaveStatus oldStatus, LeaveStatus newStatus) {
        int leaveDays = calculateLeaveDays(leave);
        String leaveType = leave.getLeaveType().name();
        Employee employee = leave.getEmployee();

        log.info("Handling quota update for leave {}: {} -> {}, days: {}, type: {}",
                leave.getId(), oldStatus, newStatus, leaveDays, leaveType);

        if (oldStatus == LeaveStatus.APPROVED && newStatus != LeaveStatus.APPROVED) {
            log.info("Removing {} days from quota (status changed from APPROVED)", leaveDays);
            // Remove from quota (rejected or cancelled) - based on isPaid status
            if (leave.getIsPaid()) {
                // Was overutilized - restore from overutilized
                restoreFromOverUtilized(employee, leaveType, leaveDays);
            } else {
                // Was within quota - restore to regular quota
                restoreToLeaveQuota(employee, leaveType, leaveDays);
            }
            leave.setIsPaid(false);

        } else if (oldStatus != LeaveStatus.APPROVED && newStatus == LeaveStatus.APPROVED) {
            log.info("Adding {} days to quota (status changed to APPROVED)", leaveDays);
            // Add to quota (approved) - check if overutilized
            boolean isPaid = isLeaveOverutilized(employee, leaveType, leaveDays);
            leave.setIsPaid(isPaid);

            if (isPaid) {
                log.info("Leave is overutilized - adding to overutilized count");
                // Overutilized - add to overutilized count
                addToOverUtilized(employee, leaveType, leaveDays);
            } else {
                log.info("Leave is within quota - deducting from regular quota");
                // Within quota - deduct from regular quota
                deductFromLeaveQuota(employee, leaveType, leaveDays);
            }
        }
    }

    private void addToOverUtilized(Employee employee, String leaveType, int days) {
        LeaveQuota quota = leaveQuotaRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException("Leave quota not found for type: " + leaveType));

        // Directly update the overUtilized field
        quota.setOverUtilized(quota.getOverUtilized() + days);
        // Also update totalTaken to maintain consistency
        quota.setTotalTaken(quota.getTotalTaken() + days);

        // Recalculate remaining leaves
        quota.calculateRemaining();

        leaveQuotaRepository.save(quota);

        log.info("Added {} days to overutilized count for employee {} ({}), total overutilized: {}",
                days, employee.getEmployeeId(), leaveType, quota.getOverUtilized());
    }

    private void restoreToLeaveQuota(Employee employee, String leaveType, int days) {
        LeaveQuota quota = leaveQuotaRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException("Leave quota not found for type: " + leaveType));

        quota.setTotalTaken(Math.max(0, quota.getTotalTaken() - days));

        // Recalculate remaining leaves and overutilized
        quota.calculateRemaining();

        leaveQuotaRepository.save(quota);
    }

    private void restoreFromOverUtilized(Employee employee, String leaveType, int days) {
        LeaveQuota quota = leaveQuotaRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException("Leave quota not found for type: " + leaveType));

        int restoredDays = Math.min(quota.getOverUtilized(), days);
        quota.setOverUtilized(quota.getOverUtilized() - restoredDays);
        leaveQuotaRepository.save(quota);

        log.info("Restored {} days from overutilized count for employee {} ({}), remaining overutilized: {}",
                restoredDays, employee.getEmployeeId(), leaveType, quota.getOverUtilized());
    }

    private boolean isLeaveOverutilized(Employee employee, String leaveType, int requestedDays) {
        LeaveQuota quota = leaveQuotaRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException("Leave quota not found for type: " + leaveType));

        log.info("Checking quota for {}: available={}, requested={}",
                employee.getEmployeeId(), quota.getRemainingLeaves(), requestedDays);

        // Check if the requested days would exceed the remaining leaves
        if (quota.getRemainingLeaves() < requestedDays) {
            log.info("Leave is overutilized - marking as paid");
            return true;
        }

        log.info("Leave is within quota - marking as not paid");
        return false;
    }

    private void validateLeaveRequest(LeaveRequestDto requestDto) {
        if (requestDto.getDurationType() == DurationType.MULTIPLE) {
            if (requestDto.getStartDate() == null || requestDto.getEndDate() == null) {
                throw new IllegalArgumentException("Start date and end date are required for multiple days leave");
            }
            if (requestDto.getStartDate().isAfter(requestDto.getEndDate())) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }
        } else {
            if (requestDto.getSingleDate() == null) {
                throw new IllegalArgumentException("Date is required for single day leave");
            }
        }
    }

    private void validateAdminLeaveRequest(AdminLeaveRequestDto requestDto) {
        if (requestDto.getDurationType() == DurationType.MULTIPLE) {
            if (requestDto.getStartDate() == null || requestDto.getEndDate() == null) {
                throw new IllegalArgumentException("Start date and end date are required for multiple days leave");
            }
            if (requestDto.getStartDate().isAfter(requestDto.getEndDate())) {
                throw new IllegalArgumentException("Start date cannot be after end date");
            }
        } else {
            if (requestDto.getSingleDate() == null) {
                throw new IllegalArgumentException("Date is required for single day leave");
            }
        }

        if (requestDto.getEmployeeIds() == null || requestDto.getEmployeeIds().isEmpty()) {
            throw new IllegalArgumentException("At least one employee must be selected");
        }
    }

    private Leave createLeaveFromRequest(Employee employee, LeaveRequestDto requestDto) {
        Leave leave = new Leave();
        leave.setEmployee(employee);
        leave.setLeaveType(requestDto.getLeaveType());
        leave.setDurationType(requestDto.getDurationType());
        leave.setStartDate(requestDto.getStartDate());
        leave.setEndDate(requestDto.getEndDate());
        leave.setSingleDate(requestDto.getSingleDate());
        leave.setReason(requestDto.getReason());
        leave.setDocuments(new ArrayList<>());
        leave.setCreatedAt(LocalDateTime.now());
        return leave;
    }

    private Leave createLeaveFromAdminRequest(Employee employee, AdminLeaveRequestDto requestDto) {
        Leave leave = new Leave();
        leave.setEmployee(employee);
        leave.setLeaveType(requestDto.getLeaveType());
        leave.setDurationType(requestDto.getDurationType());
        leave.setStartDate(requestDto.getStartDate());
        leave.setEndDate(requestDto.getEndDate());
        leave.setSingleDate(requestDto.getSingleDate());
        leave.setReason(requestDto.getReason());
        leave.setDocuments(new ArrayList<>());
        leave.setCreatedAt(LocalDateTime.now());
        return leave;
    }

    private int calculateLeaveDays(LeaveRequestDto requestDto) {
        if (requestDto.getDurationType() == DurationType.MULTIPLE) {
            return (int) ChronoUnit.DAYS.between(
                    requestDto.getStartDate(), requestDto.getEndDate()) + 1;
        } else {
            // For half-day leaves, count as 0.5 days
            return (requestDto.getDurationType() == DurationType.FULL_DAY) ? 1 : 0;
        }
    }

    private int calculateAdminLeaveDays(AdminLeaveRequestDto requestDto) {
        if (requestDto.getDurationType() == DurationType.MULTIPLE) {
            return (int) ChronoUnit.DAYS.between(
                    requestDto.getStartDate(), requestDto.getEndDate()) + 1;
        } else {
            // For half-day leaves, count as 0.5 days
            return (requestDto.getDurationType() == DurationType.FULL_DAY) ? 1 : 0;
        }
    }

    private int calculateLeaveDays(Leave leave) {
        if (leave.getDurationType() == DurationType.MULTIPLE) {
            return (int) ChronoUnit.DAYS.between(
                    leave.getStartDate(), leave.getEndDate()) + 1;
        } else {
            return (leave.getDurationType() == DurationType.FULL_DAY) ? 1 : 0;
        }
    }

    private void validateLeaveQuota(Employee employee, String leaveType, int requestedDays) {
        LeaveQuota quota = leaveQuotaRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException("Leave quota not found for type: " + leaveType));

        if (quota.getRemainingLeaves() < requestedDays) {
            throw new IllegalArgumentException("Insufficient leave balance. Available: " +
                    quota.getRemainingLeaves() + ", Requested: " + requestedDays);
        }
    }

    private void deductFromLeaveQuota(Employee employee, String leaveType, int days) {
        LeaveQuota quota = leaveQuotaRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseThrow(() -> new ResourceNotFoundException("Leave quota not found for type: " + leaveType));

        quota.setTotalTaken(quota.getTotalTaken() + days);
        quota.setRemainingLeaves(quota.getTotalLeaves() - quota.getTotalTaken());

        if (quota.getRemainingLeaves() < 0) {
            quota.setOverUtilized(Math.abs(quota.getRemainingLeaves()));
            quota.setRemainingLeaves(0);
        }

        leaveQuotaRepository.save(quota);
    }

    private List<FileMeta> uploadLeaveDocuments(List<MultipartFile> files, Employee employee, String entityType) {
        List<FileMeta> uploadedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String folderPath = "leaves/" + employee.getEmployeeId() + "/" + System.currentTimeMillis();
                FileMeta fileMeta = supabaseStorageService.uploadFile(file, folderPath, employee.getEmployeeId());
                fileMeta.setEmployee(employee);
                fileMeta.setEntityType(entityType);

                // Save file metadata to database
                FileMeta savedFileMeta = fileMetaService.save(fileMeta);
                uploadedFiles.add(savedFileMeta);

            } catch (Exception e) {
                log.error("Failed to upload leave document for employee {}: {}",
                        employee.getEmployeeId(), e.getMessage());
                // Continue with other files even if one fails
            }
        }

        return uploadedFiles;
    }

    private void deleteLeaveDocuments(List<FileMeta> documents) {
        if (documents != null) {
            for (FileMeta document : documents) {
                try {
                    // Delete from Supabase storage
                    supabaseStorageService.deleteFile(document.getPath());
                    // Delete from database
                    fileMetaService.deleteById(document.getId());
                } catch (Exception e) {
                    log.error("Failed to delete leave document {}: {}", document.getId(), e.getMessage());
                }
            }
        }
    }

    private LeaveResponseDto convertToDto(Leave leave) {
        LeaveResponseDto dto = new LeaveResponseDto();
        dto.setId(leave.getId());
        dto.setEmployeeId(leave.getEmployee().getEmployeeId());
        dto.setEmployeeName(leave.getEmployee().getName());
        dto.setLeaveType(leave.getLeaveType());
        dto.setDurationType(leave.getDurationType());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setSingleDate(leave.getSingleDate());
        dto.setReason(leave.getReason());
        dto.setStatus(leave.getStatus());
        dto.setIsPaid(leave.getIsPaid()); // Add this line
        dto.setRejectionReason(leave.getRejectionReason());
        dto.setApprovedByName(leave.getApprovedBy() != null ? leave.getApprovedBy().getName() : null);
        dto.setApprovedAt(leave.getApprovedAt());
        dto.setRejectedAt(leave.getRejectedAt());

        // Add document URLs
        if (leave.getDocuments() != null) {
            dto.setDocumentUrls(leave.getDocuments().stream()
                    .map(FileMeta::getUrl)
                    .collect(Collectors.toList()));
        }

        dto.setCreatedAt(leave.getCreatedAt());
        dto.setUpdatedAt(leave.getUpdatedAt());
        return dto;
    }

    private EmployeeOnLeaveDto convertToEmployeeOnLeaveDto(Leave leave) {
        EmployeeOnLeaveDto dto = new EmployeeOnLeaveDto();
        dto.setEmployeeId(leave.getEmployee().getEmployeeId());
        dto.setEmployeeName(leave.getEmployee().getName());
        dto.setDepartment(leave.getEmployee().getDepartment() != null ?
                leave.getEmployee().getDepartment().getDepartmentName() : "N/A");
        dto.setLeaveType(leave.getLeaveType());
        return dto;
    }

    private void notifyAdminsAboutNewLeave(Employee employee, Leave leave) {
        List<Employee> admins = employeeRepository.findByRole("ROLE_ADMIN");

        if (admins != null && !admins.isEmpty()) {
            List<String> adminIds = admins.stream()
                    .map(Employee::getEmployeeId)
                    .collect(Collectors.toList());

            String title = "New Leave Application";
            String message = String.format("%s has applied for %s leave from %s to %s",
                    employee.getName(),
                    leave.getLeaveType().name(),
                    leave.getDurationType() == DurationType.MULTIPLE ? leave.getStartDate() : leave.getSingleDate(),
                    leave.getDurationType() == DurationType.MULTIPLE ? leave.getEndDate() : leave.getSingleDate());

            notificationService.sendNotificationMany(
                    employee.getEmployeeId(),
                    adminIds,
                    title,
                    message,
                    "LEAVE_APPLICATION"
            );
        }
    }

    private void notifyEmployeeAboutLeave(Employee employee, Leave leave, Employee admin) {
        try {
            String title = "Leave " + leave.getStatus().toString();
            String message = String.format("Your %s leave from %s to %s has been %s by %s",
                    leave.getLeaveType().name(),
                    leave.getDurationType() == DurationType.MULTIPLE ? leave.getStartDate() : leave.getSingleDate(),
                    leave.getDurationType() == DurationType.MULTIPLE ? leave.getEndDate() : leave.getSingleDate(),
                    leave.getStatus().toString().toLowerCase(),
                    admin.getName());

            if (leave.getStatus() == LeaveStatus.REJECTED && leave.getRejectionReason() != null) {
                message += ". Reason: " + leave.getRejectionReason();
            }

            SendNotificationDto notificationDto = new SendNotificationDto();
            notificationDto.setReceiverEmployeeId(employee.getEmployeeId());
            notificationDto.setTitle(title);
            notificationDto.setMessage(message);
            notificationDto.setType("LEAVE_STATUS_UPDATE");

            notificationService.sendNotification(
                    admin.getEmployeeId(),
                    notificationDto
            );
        } catch (Exception e) {
            log.error("Failed to send leave status notification to employee {}: {}",
                    employee.getEmployeeId(), e.getMessage());
        }
    }

    private void notifyEmployeeAboutStatusChange(Employee employee, Leave leave, Employee admin) {
        try {
            String title = "Leave Status Updated";
            String message = String.format("Your %s leave from %s to %s has been %s",
                    leave.getLeaveType().name(),
                    leave.getDurationType() == DurationType.MULTIPLE ? leave.getStartDate() : leave.getSingleDate(),
                    leave.getDurationType() == DurationType.MULTIPLE ? leave.getEndDate() : leave.getSingleDate(),
                    leave.getStatus().toString().toLowerCase());

            if (leave.getStatus() == LeaveStatus.REJECTED && leave.getRejectionReason() != null) {
                message += ". Reason: " + leave.getRejectionReason();
            }

            SendNotificationDto notificationDto = new SendNotificationDto();
            notificationDto.setReceiverEmployeeId(employee.getEmployeeId());
            notificationDto.setTitle(title);
            notificationDto.setMessage(message);
            notificationDto.setType("LEAVE_STATUS_UPDATE");

            notificationService.sendNotification(
                    admin.getEmployeeId(),
                    notificationDto
            );
        } catch (Exception e) {
            log.error("Failed to send leave status change notification to employee {}: {}",
                    employee.getEmployeeId(), e.getMessage());
        }
    }

    public LeaveResponseDto getLeaveById(Long leaveId) {
        return leaveRepository.findById(leaveId)
                .map(this::convertToDto)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Leave not found with id: " + leaveId));
    }

}