package com.erp.client_service.service.document;

import com.erp.client_service.dto.document.DocumentResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface DocumentService {
    DocumentResponseDto uploadDocument(Long clientId, MultipartFile file, String uploadedBy) throws IOException;
    List<DocumentResponseDto> listDocuments(Long clientId);
    void streamDocument(Long docId, HttpServletResponse response) throws IOException;
    void deleteDocument(Long docId);
}