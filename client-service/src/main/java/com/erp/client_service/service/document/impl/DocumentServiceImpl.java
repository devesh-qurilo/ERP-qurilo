package com.erp.client_service.service.document.impl;

import com.erp.client_service.dto.document.DocumentResponseDto;
import com.erp.client_service.entity.ClientDocument;
import com.erp.client_service.exception.ResourceNotFoundException;
import com.erp.client_service.repository.ClientDocumentRepository;

import com.erp.client_service.service.document.DocumentService;
import com.erp.client_service.service.supabase.SupabaseService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final ClientDocumentRepository repo;
    private final SupabaseService supabaseService;

    @Override
    public DocumentResponseDto uploadDocument(Long clientId, MultipartFile file, String uploadedBy) throws IOException {
        String url = supabaseService.uploadFile(file, "clients/docs/" + clientId, false);
        ClientDocument d = ClientDocument.builder()
                .clientId(clientId)
                .filename(file.getOriginalFilename())
                .url(url
                )
                .mimeType(file.getContentType())
                .size(file.getSize())
                .uploadedAt(java.time.Instant.now())
                .uploadedBy(uploadedBy)
                .build();
        ClientDocument saved = repo.save(d);
        return DocumentResponseDto.builder()
                .id(saved.getId())
                .filename(saved.getFilename())
                .url(saved.getUrl())
                .mimeType(saved.getMimeType())
                .size(saved.getSize())
                .uploadedAt(saved.getUploadedAt() == null ? null : saved.getUploadedAt().toString())
                .uploadedBy(saved.getUploadedBy())
                .build();
    }

    @Override
    public List<DocumentResponseDto> listDocuments(Long clientId) {
        return repo.findByClientId(clientId).stream().map(d -> DocumentResponseDto.builder()
                .id(d.getId())
                .filename(d.getFilename())
                .url(d.getUrl())
                .mimeType(d.getMimeType())
                .size(d.getSize())
                .uploadedAt(d.getUploadedAt() == null ? null : d.getUploadedAt().toString())
                .uploadedBy(d.getUploadedBy())
                .build()).collect(Collectors.toList());
    }

    @Override
    public void streamDocument(Long docId, HttpServletResponse response) throws IOException {
        ClientDocument d = repo.findById(docId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        // Proxy by redirecting to supabase public URL
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + d.getFilename().replaceAll("\"","") + "\"");
        response.setContentType(d.getMimeType() == null ? "application/octet-stream" : d.getMimeType());
        response.sendRedirect(d.getUrl());
    }

    @Override
    public void deleteDocument(Long docId) {
        if (!repo.existsById(docId)) throw new ResourceNotFoundException("Document not found");
        repo.deleteById(docId);
    }
}
