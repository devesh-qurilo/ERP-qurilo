package com.erp.employee_service.service.award;

import com.erp.employee_service.entity.award.AwardIcon;
import com.erp.employee_service.repository.AwardIconRepository;
import com.erp.employee_service.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AwardIconService {

    private final AwardIconRepository awardIconRepository;
    private final SupabaseStorageService storageService;

    /**
     * Uploads an award icon to Supabase and saves metadata to database
     */
    public AwardIcon uploadAwardIcon(MultipartFile file, String folder, String uploadedBy) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Award icon file is required");
            }

            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String safeFilename = UUID.randomUUID() + fileExtension;

            // Create object path - ensure folder is properly formatted
            String objectPath = (folder.endsWith("/") ? folder : folder + "/") + safeFilename;

            // Upload to Supabase - use the dedicated award icon method
            String publicUrl = storageService.uploadAwardIcon(file, objectPath);

            // Create and save AwardIcon entity
            AwardIcon awardIcon = AwardIcon.builder()
                    .bucket("awards")
                    .path(objectPath)
                    .filename(originalFilename)
                    .mime(file.getContentType())
                    .size(file.getSize())
                    .url(publicUrl)
                    .uploadedBy(uploadedBy)
                    .uploadedAt(java.time.LocalDateTime.now())
                    .build();

            return awardIconRepository.save(awardIcon);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to upload award icon: " + ex.getMessage(), ex);
        }
    }

    /**
     * Deletes an award icon from Supabase and database
     */
    public void deleteAwardIcon(AwardIcon awardIcon) {
        if (awardIcon == null) return;

        try {
            // Delete from Supabase storage
            storageService.deleteFile(awardIcon.getPath());

            // Delete from database
            awardIconRepository.delete(awardIcon);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete award icon: " + ex.getMessage(), ex);
        }
    }

    /**
     * Finds an award icon by ID
     */
    public AwardIcon findAwardIconById(Long id) {
        return awardIconRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Award icon not found with ID: " + id));
    }
}