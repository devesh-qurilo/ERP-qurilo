package com.erp.employee_service.controller.award;

import com.erp.employee_service.dto.award.AwardRequestDto;
import com.erp.employee_service.dto.award.AwardResponseDto;
import com.erp.employee_service.service.award.AwardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/employee/api/awards")
@RequiredArgsConstructor
public class AwardController {

    private final AwardService awardService;

//    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<AwardResponseDto> createAward(
//            @Valid @ModelAttribute AwardRequestDto requestDto) {
//        // Using a default system user ID since awards aren't assigned to employees
//        String systemUserId = "system-admin";
//        AwardResponseDto response = awardService.createAward(requestDto, systemUserId);
//        return ResponseEntity.ok(response);
//    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AwardResponseDto> createAward(
            @RequestParam("title") String title,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile) {

        AwardRequestDto dto = new AwardRequestDto();
        dto.setTitle(title);
        dto.setSummary(summary);
        dto.setIconFile(iconFile);

        String systemUserId = "system-admin";
        AwardResponseDto response = awardService.createAward(dto, systemUserId);
        return ResponseEntity.ok(response);
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AwardResponseDto> updateAward(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile) {

        // Build DTO from incoming multipart parts. Only set fields you receive.
        AwardRequestDto dto = new AwardRequestDto();
        if (title != null) dto.setTitle(title);
        if (summary != null) dto.setSummary(summary);
        dto.setIconFile(iconFile);

        String systemUserId = "system-admin";
        AwardResponseDto response = awardService.updateAward(id, dto, systemUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AwardResponseDto>> getAllAwards() {
        // Only admins can see all awards (both active and inactive)
        List<AwardResponseDto> awards = awardService.getAllAwards();
        return ResponseEntity.ok(awards);
    }

    @GetMapping("/active")
    public ResponseEntity<List<AwardResponseDto>> getActiveAwards() {
        // Both admins and employees can see active awards
        List<AwardResponseDto> awards = awardService.getActiveAwards();
        return ResponseEntity.ok(awards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AwardResponseDto> getAwardById(@PathVariable Long id, Authentication authentication) {
        // Check if user is admin
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));

        AwardResponseDto award;
        if (isAdmin) {
            // Admin can see all awards
            award = awardService.getAwardById(id);
        } else {
            // Employee can only see active awards
            award = awardService.getActiveAwardById(id);
        }

        return ResponseEntity.ok(award);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAward(@PathVariable Long id) {
        awardService.deleteAward(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AwardResponseDto> toggleAwardStatus(@PathVariable Long id) {
        AwardResponseDto response = awardService.toggleAwardStatus(id);
        return ResponseEntity.ok(response);
    }
}