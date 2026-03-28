package com.erp.lead_service.service;

import com.erp.lead_service.dto.DealDocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DealDocumentService {
    DealDocumentDto uploadDocument(Long dealId, MultipartFile file, String authHeader);
    List<DealDocumentDto> listDocuments(Long dealId, String authHeader);
    void deleteDocument(Long dealId, Long documentId, String authHeader);
    DealDocumentDto getDocument(Long dealId, Long documentId, String authHeader);
}
