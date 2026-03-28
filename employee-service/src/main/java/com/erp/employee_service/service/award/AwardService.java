package com.erp.employee_service.service.award;

import com.erp.employee_service.dto.award.AwardRequestDto;
import com.erp.employee_service.dto.award.AwardResponseDto;
import com.erp.employee_service.entity.award.Award;
import com.erp.employee_service.entity.award.AwardIcon;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.AwardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AwardService {

    private final AwardRepository awardRepository;
    private final AwardIconService awardIconService;

    public AwardResponseDto createAward(AwardRequestDto requestDto, String uploadedByEmployeeId) {
        Award award = Award.builder()
                .title(requestDto.getTitle())
                .summary(requestDto.getSummary())
                .isActive(true) // Default to active when created
                .uploadedByEmployeeId(uploadedByEmployeeId)
                .build();

        // Handle icon upload if present
        if (requestDto.getIconFile() != null && !requestDto.getIconFile().isEmpty()) {
            AwardIcon awardIcon = awardIconService.uploadAwardIcon(
                    requestDto.getIconFile(),
                    "awards/icons",
                    uploadedByEmployeeId
            );
            award.setIcon(awardIcon);
        }

        Award savedAward = awardRepository.save(award);
        return mapToResponseDto(savedAward);
    }

    public AwardResponseDto updateAward(Long id, AwardRequestDto requestDto, String uploadedByEmployeeId) {
        Award award = awardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award not found with ID: " + id));

        award.setTitle(requestDto.getTitle());
        award.setSummary(requestDto.getSummary());
        award.setUploadedByEmployeeId(uploadedByEmployeeId);

        // Handle icon update if new file is provided
        if (requestDto.getIconFile() != null && !requestDto.getIconFile().isEmpty()) {
            // Delete old icon if exists
            if (award.getIcon() != null) {
                awardIconService.deleteAwardIcon(award.getIcon());
            }

            // Upload new icon
            AwardIcon newAwardIcon = awardIconService.uploadAwardIcon(
                    requestDto.getIconFile(),
                    "awards/icons",
                    uploadedByEmployeeId
            );
            award.setIcon(newAwardIcon);
        }

        Award updatedAward = awardRepository.save(award);
        return mapToResponseDto(updatedAward);
    }

    public List<AwardResponseDto> getAllAwards() {
        return awardRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<AwardResponseDto> getActiveAwards() {
        return awardRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public AwardResponseDto getAwardById(Long id) {
        Award award = awardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award not found with ID: " + id));
        return mapToResponseDto(award);
    }

    public AwardResponseDto getActiveAwardById(Long id) {
        Award award = awardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award not found with ID: " + id));

        if (!award.getIsActive()) {
            throw new AccessDeniedException("Access denied to inactive award");
        }

        return mapToResponseDto(award);
    }

    public void deleteAward(Long id) {
        Award award = awardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award not found with ID: " + id));

        // Delete associated icon
        if (award.getIcon() != null) {
            awardIconService.deleteAwardIcon(award.getIcon());
        }

        awardRepository.delete(award);
    }

    public AwardResponseDto toggleAwardStatus(Long id) {
        Award award = awardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award not found with ID: " + id));

        // Toggle the status
        award.setIsActive(!award.getIsActive());

        Award updatedAward = awardRepository.save(award);
        return mapToResponseDto(updatedAward);
    }

    private AwardResponseDto mapToResponseDto(Award award) {
        AwardResponseDto dto = new AwardResponseDto();
        dto.setId(award.getId());
        dto.setTitle(award.getTitle());
        dto.setSummary(award.getSummary());
        dto.setIsActive(award.getIsActive());
        dto.setCreatedAt(award.getCreatedAt());
        dto.setUpdatedAt(award.getUpdatedAt());
        if (award.getIcon() != null) {
            dto.setIconUrl(award.getIcon().getUrl());
            dto.setIconFileId(award.getIcon().getId());
        }

        return dto;
    }
}