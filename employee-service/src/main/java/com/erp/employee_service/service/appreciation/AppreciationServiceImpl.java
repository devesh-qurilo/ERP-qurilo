package com.erp.employee_service.service.appreciation;

import com.erp.employee_service.dto.appreciation.AppreciationRequestDto;
import com.erp.employee_service.dto.appreciation.AppreciationResponseDto;
import com.erp.employee_service.entity.FileMeta;
import com.erp.employee_service.entity.appreciation.Appreciation;
import com.erp.employee_service.entity.award.Award;
import com.erp.employee_service.entity.Employee;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.FileMetaRepository;
import com.erp.employee_service.repository.EmployeeRepository;
import com.erp.employee_service.repository.AppreciationRepository;
import com.erp.employee_service.repository.AwardRepository;
import com.erp.employee_service.service.notification.NotificationService;
import com.erp.employee_service.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppreciationServiceImpl implements AppreciationService {

    private final AppreciationRepository repo;
    private final AwardRepository awardRepo;
    private final EmployeeRepository employeeRepo;
    private final FileMetaRepository fileMetaRepo;
    private final SupabaseStorageService supabase;
    private final NotificationService notificationService;

    @Override
    public AppreciationResponseDto create(String adminEmployeeId, AppreciationRequestDto dto) {
        Award award = awardRepo.findById(dto.getAwardId())
                .orElseThrow(() -> new ResourceNotFoundException("Award not found"));
        Employee givenTo = employeeRepo.findByEmployeeId(dto.getGivenToEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Appreciation a = new Appreciation();
        a.setAward(award);
        a.setGivenTo(givenTo);
        a.setDate(dto.getDate());
        a.setSummary(dto.getSummary());
        a.setIsActive(Boolean.TRUE);
        a.setCreatedAt(LocalDateTime.now());

        // handle photo upload if present
        MultipartFile photo = dto.getPhotoFile();
        if (photo != null && !photo.isEmpty()) {
            // Choose folder pattern, e.g., "appreciations/EMP001"
            String folder = "appreciations/" + givenTo.getEmployeeId();
            FileMeta meta = supabase.uploadFile(photo, folder, adminEmployeeId);
            // link the employee and entityType and persist meta
            meta.setEmployee(givenTo);
            meta.setEntityType("APPRECIATION_PHOTO");
            fileMetaRepo.save(meta);
            a.setPhoto(meta);
        }

        Appreciation saved = repo.save(a);
        // send notification to the user asynchronously (service handles @Async)
        String title = "You received an appreciation!";
        String message = "You were appreciated for " + award.getTitle() + (dto.getSummary() != null ? ": " + dto.getSummary() : "");
        com.erp.employee_service.dto.notification.SendNotificationDto sendDto =
                new com.erp.employee_service.dto.notification.SendNotificationDto();
        sendDto.setReceiverEmployeeId(givenTo.getEmployeeId());
        sendDto.setTitle(title);
        sendDto.setMessage(message);
        sendDto.setType("APPRECIATION");

        notificationService.sendNotification(adminEmployeeId, sendDto);

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppreciationResponseDto> getAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppreciationResponseDto> getForEmployee(String employeeId) {
        return repo.findByGivenTo_EmployeeIdOrderByDateDesc(employeeId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppreciationResponseDto getById(Long id) {
        Appreciation a = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Appreciation not found"));
        return toDto(a);
    }

    @Override
    public AppreciationResponseDto update(String adminEmployeeId, Long id, AppreciationRequestDto dto) {
        Appreciation a = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Appreciation not found"));

        // update award if changed
        if (dto.getAwardId() != null && !dto.getAwardId().equals(a.getAward().getId())) {
            Award award = awardRepo.findById(dto.getAwardId()).orElseThrow(() -> new ResourceNotFoundException("Award not found"));
            a.setAward(award);
        }

        // update givenTo (rare) - allow admin
        if (dto.getGivenToEmployeeId() != null && !dto.getGivenToEmployeeId().equals(a.getGivenTo().getEmployeeId())) {
            Employee newEmp = employeeRepo.findByEmployeeId(dto.getGivenToEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
            a.setGivenTo(newEmp);
        }

        if (dto.getDate() != null) a.setDate(dto.getDate());
        a.setSummary(dto.getSummary());
        a.setIsActive(Boolean.TRUE);
        a.setUpdatedAt(LocalDateTime.now());

        // replace photo if provided
        MultipartFile photo = dto.getPhotoFile();
        if (photo != null && !photo.isEmpty()) {
            // delete old photo if present
            if (a.getPhoto() != null) {
                try {
                    supabase.deleteFile(a.getPhoto().getPath());
                } catch (Exception ex) {
                    log.warn("failed to delete old appreciation photo: {}", ex.getMessage());
                }
                fileMetaRepo.delete(a.getPhoto());
            }
            String folder = "appreciations/" + a.getGivenTo().getEmployeeId();
            FileMeta meta = supabase.uploadFile(photo, folder, adminEmployeeId);
            meta.setEmployee(a.getGivenTo());
            meta.setEntityType("APPRECIATION_PHOTO");
            fileMetaRepo.save(meta);
            a.setPhoto(meta);
        }

        Appreciation updated = repo.save(a);
        return toDto(updated);
    }

    @Override
    public void delete(String adminEmployeeId, Long id) {
        Appreciation a = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Appreciation not found"));
        // delete photo from storage and meta row if exists
        if (a.getPhoto() != null) {
            try {
                supabase.deleteFile(a.getPhoto().getPath());
            } catch (Exception ex) {
                log.warn("Failed to delete supabase object: {}", ex.getMessage());
            }
            fileMetaRepo.delete(a.getPhoto());
        }
        repo.delete(a);
    }

    private AppreciationResponseDto toDto(Appreciation a) {
        AppreciationResponseDto d = new AppreciationResponseDto();
        d.setId(a.getId());
        d.setAwardId(a.getAward() != null ? a.getAward().getId() : null);
        d.setAwardTitle(a.getAward() != null ? a.getAward().getTitle() : null);
        d.setGivenToEmployeeId(a.getGivenTo() != null ? a.getGivenTo().getEmployeeId() : null);
        d.setGivenToEmployeeName(a.getGivenTo() != null ? a.getGivenTo().getName() : null);
        d.setDate(a.getDate());
        d.setSummary(a.getSummary());
        d.setIsActive(a.getIsActive());
        if (a.getPhoto() != null) {
            d.setPhotoUrl(a.getPhoto().getUrl());
            d.setPhotoFileId(a.getPhoto().getId());
        }
        d.setCreatedAt(a.getCreatedAt());
        d.setUpdatedAt(a.getUpdatedAt());
        return d;
    }
}
