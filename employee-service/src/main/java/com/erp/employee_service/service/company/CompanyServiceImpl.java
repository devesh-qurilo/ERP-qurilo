package com.erp.employee_service.service.company;

import com.erp.employee_service.dto.company.CompanyRequestDto;
import com.erp.employee_service.dto.company.CompanyResponseDto;
import com.erp.employee_service.entity.award.AwardIcon;
import com.erp.employee_service.entity.company.Company;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.AwardIconRepository;
import com.erp.employee_service.repository.CompanyRepository;
import com.erp.employee_service.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepo;
    private final AwardIconRepository awardIconRepo;
    private final SupabaseStorageService supabase;

    @Override
    public CompanyResponseDto createCompany(String adminEmployeeId, CompanyRequestDto dto) {
        // Check if company already exists
        Optional<Company> existingCompany = companyRepo.findFirstByIsActiveTrue();
        if (existingCompany.isPresent()) {
            throw new IllegalStateException("Company already exists. Use update instead.");
        }

        // Validate unique constraints
        if (companyRepo.existsByCompanyName(dto.getCompanyName())) {
            throw new IllegalArgumentException("Company name already exists");
        }
        if (companyRepo.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Company company = new Company();
        company.setCompanyName(dto.getCompanyName());
        company.setEmail(dto.getEmail());
        company.setContactNo(dto.getContactNo());
        company.setWebsite(dto.getWebsite());
        company.setAddress(dto.getAddress());
        company.setIsActive(true);

        // Handle logo upload
        MultipartFile logoFile = dto.getLogoFile();
        if (logoFile != null && !logoFile.isEmpty()) {
            AwardIcon logo = uploadLogo(logoFile, adminEmployeeId);
            company.setLogo(logo);
        }

        Company saved = companyRepo.save(company);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponseDto getCompany() {
        Company company = companyRepo.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        return toDto(company);
    }

    @Override
    public CompanyResponseDto updateCompany(String adminEmployeeId, CompanyRequestDto dto) {
        Company company = companyRepo.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        // Update fields if provided
        if (dto.getCompanyName() != null && !dto.getCompanyName().equals(company.getCompanyName())) {
            if (companyRepo.existsByCompanyName(dto.getCompanyName())) {
                throw new IllegalArgumentException("Company name already exists");
            }
            company.setCompanyName(dto.getCompanyName());
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(company.getEmail())) {
            if (companyRepo.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            company.setEmail(dto.getEmail());
        }

        if (dto.getContactNo() != null) company.setContactNo(dto.getContactNo());
        if (dto.getWebsite() != null) company.setWebsite(dto.getWebsite());
        if (dto.getAddress() != null) company.setAddress(dto.getAddress());
        company.setUpdatedAt(LocalDateTime.now());

        // Handle logo update
        MultipartFile logoFile = dto.getLogoFile();
        if (logoFile != null && !logoFile.isEmpty()) {
            // Delete old logo if exists
            if (company.getLogo() != null) {
                deleteLogo(company.getLogo());
            }
            AwardIcon newLogo = uploadLogo(logoFile, adminEmployeeId);
            company.setLogo(newLogo);
        }

        Company updated = companyRepo.save(company);
        return toDto(updated);
    }

    @Override
    public void deleteCompany(String adminEmployeeId) {
        Company company = companyRepo.findFirstByIsActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        // Delete logo if exists
        if (company.getLogo() != null) {
            deleteLogo(company.getLogo());
        }

        companyRepo.delete(company);
    }

    private AwardIcon uploadLogo(MultipartFile file, String uploadedBy) {
        try {
            // Sanitize filename and create a safe object path
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = sanitizeFilename(originalFilename);
            String objectPath = "company/logo/" + System.currentTimeMillis() + "-" + sanitizedFilename;

            // Upload to Supabase
            String publicUrl = supabase.uploadAwardIcon(file, objectPath);

            // Create and save AwardIcon entity
            AwardIcon awardIcon = new AwardIcon();
            awardIcon.setBucket("Company-Logo"); // Make sure this matches your Supabase bucket name
            awardIcon.setPath(objectPath);
            awardIcon.setFilename(originalFilename); // Store original filename
            awardIcon.setMime(file.getContentType());
            awardIcon.setSize(file.getSize());
            awardIcon.setUrl(publicUrl);
            awardIcon.setUploadedBy(uploadedBy);
            awardIcon.setUploadedAt(LocalDateTime.now());

            return awardIconRepo.save(awardIcon);
        } catch (Exception e) {
            log.error("Failed to upload company logo", e);
            throw new RuntimeException("Logo upload failed: " + e.getMessage());
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "file";
        }

        // Remove path traversal attempts
        String safeName = filename.replaceAll("\\.\\./", "").replaceAll("\\.\\.\\\\", "");

        // Replace spaces and special characters with underscores or remove them
        safeName = safeName.replaceAll("[^a-zA-Z0-9.-]", "_");

        // Limit length to avoid issues
        if (safeName.length() > 100) {
            String extension = "";
            int dotIndex = safeName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = safeName.substring(dotIndex);
                safeName = safeName.substring(0, dotIndex);
            }
            safeName = safeName.substring(0, Math.min(100 - extension.length(), safeName.length())) + extension;
        }

        return safeName;
    }

    private void deleteLogo(AwardIcon logo) {
        try {
            // Delete from Supabase
            supabase.deleteFile(logo.getPath());
            // Delete from database
            awardIconRepo.delete(logo);
        } catch (Exception e) {
            log.warn("Failed to delete logo: {}", e.getMessage());
        }
    }

    private CompanyResponseDto toDto(Company company) {
        CompanyResponseDto dto = new CompanyResponseDto();
        dto.setId(company.getId());
        dto.setCompanyName(company.getCompanyName());
        dto.setEmail(company.getEmail());
        dto.setContactNo(company.getContactNo());
        dto.setWebsite(company.getWebsite());
        dto.setAddress(company.getAddress());
        dto.setIsActive(company.getIsActive());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setUpdatedAt(company.getUpdatedAt());

        if (company.getLogo() != null) {
            dto.setLogoUrl(company.getLogo().getUrl());
            dto.setLogoId(company.getLogo().getId());
        }

        return dto;
    }
}