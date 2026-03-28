package com.erp.lead_service.service;

import com.erp.lead_service.client.EmployeeServiceClient;
import com.erp.lead_service.dto.EmployeeMetaDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.DealDocument;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.repository.DealDocumentRepository;
import com.erp.lead_service.repository.DealRepository;
import com.erp.lead_service.service.DealDocumentService;
import com.erp.lead_service.service.SupabaseService;
import com.erp.lead_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DealDocumentServiceImpl implements DealDocumentService {

    private final DealRepository dealRepository;
    private final DealDocumentRepository docRepo;
    private final SupabaseService supabaseService;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public com.erp.lead_service.dto.DealDocumentDto uploadDocument(Long dealId, MultipartFile file, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access deal employees");
        }
        Deal deal = dealRepository.findById(dealId).orElseThrow(() -> new ResourceNotFoundException("Deal not found"));

        // upload to supabase, returns publicUrl
        String publicUrl = supabaseService.uploadFile(file, deal.getId());
        DealDocument d = new DealDocument();
        d.setDeal(deal);
        d.setFilename(file.getOriginalFilename());
        d.setUrl(publicUrl);
        // uploadedBy - extract from token in controller
        DealDocument saved = docRepo.save(d);
        com.erp.lead_service.dto.DealDocumentDto dto = new com.erp.lead_service.dto.DealDocumentDto();
        dto.setId(saved.getId());
        dto.setFilename(saved.getFilename());
        dto.setUrl(saved.getUrl());
        dto.setUploadedAt(saved.getUploadedAt());
        return dto;
    }

    @Override
    public List<com.erp.lead_service.dto.DealDocumentDto> listDocuments(Long dealId, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access deal employees");
        }
        return docRepo.findByDealId(dealId).stream().map(doc -> {
            com.erp.lead_service.dto.DealDocumentDto dto = new com.erp.lead_service.dto.DealDocumentDto();
            dto.setId(doc.getId());
            dto.setFilename(doc.getFilename());
            dto.setUrl(doc.getUrl());
            dto.setUploadedAt(doc.getUploadedAt());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDocument(Long dealId, Long documentId, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access deal employees");
        }

        DealDocument d = docRepo.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (d.getDeal() == null || !d.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Document not found for this deal");
        }

        // Try to extract object path from URL first
        try {
            String objectPath = extractObjectPathFromStoredUrl(d.getUrl());
            if (objectPath != null) {
                supabaseService.deleteFileByObjectPath(objectPath);
            } else {
                // Fallback to URL-based deletion
                supabaseService.deleteFileByUrl(d.getUrl());
            }
        } catch (Exception e) {
            log.warn("Failed to delete file from Supabase (continuing with DB deletion): {}", e.getMessage());
        }

        docRepo.delete(d);
    }

    private String extractObjectPathFromStoredUrl(String url) {
        if (url == null) return null;

        // Extract path after "/object/public/bucket-name/"
        String bucketName = "ERP-BUCKET";
        String prefix = "/storage/v1/object/public/" + bucketName + "/";
        int index = url.indexOf(prefix);
        if (index != -1) {
            return url.substring(index + prefix.length());
        }
        return null;
    }

    @Override
    public com.erp.lead_service.dto.DealDocumentDto getDocument(Long dealId, Long documentId, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access deal employees");
        }
        DealDocument d = docRepo.findById(documentId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (d.getDeal() == null || !d.getDeal().getId().equals(dealId)) {
            throw new ResourceNotFoundException("Document not found for this deal");
        }
        com.erp.lead_service.dto.DealDocumentDto dto = new com.erp.lead_service.dto.DealDocumentDto();
        dto.setId(d.getId());
        dto.setFilename(d.getFilename());
        dto.setUrl(d.getUrl());
        dto.setUploadedAt(d.getUploadedAt());
        return dto;
    }

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }


}
