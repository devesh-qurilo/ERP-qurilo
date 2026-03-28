package com.erp.client_service.controller;

import com.erp.client_service.dto.document.DocumentResponseDto;

import com.erp.client_service.service.document.DocumentService;
import com.erp.client_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/clients/{clientId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final JwtUtil jwtUtil;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<DocumentResponseDto> uploadDocument(
            @PathVariable Long clientId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth
    ) throws IOException {
        String uploadedBy = jwtUtil.extractSubject(auth.substring(7));
        DocumentResponseDto dto = documentService.uploadDocument(clientId, file, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<DocumentResponseDto> listDocuments(@PathVariable Long clientId) {
        return documentService.listDocuments(clientId);
    }

    @GetMapping("/{docId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void downloadDocument(@PathVariable Long docId, HttpServletResponse response) throws IOException {
        documentService.streamDocument(docId, response);
    }

    @DeleteMapping("/{docId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteDocument(@PathVariable Long docId) {
        documentService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }
}
