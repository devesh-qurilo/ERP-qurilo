package com.erp.employee_service.controller.document;

import com.erp.employee_service.dto.FileMetaResponse;
import com.erp.employee_service.entity.FileMeta;
import com.erp.employee_service.service.document.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/employee/{employeeId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    private FileMetaResponse toDto(FileMeta m, String employeeId) {
        FileMetaResponse r = new FileMetaResponse();
        r.setId(m.getId());
        r.setBucket(m.getBucket());
        r.setPath(m.getPath());
        r.setFilename(m.getFilename());
        r.setMime(m.getMime());
        r.setSize(m.getSize());
        r.setUrl(m.getUrl());
        r.setUploadedBy(m.getUploadedBy());
        r.setEntityType(m.getEntityType());
        r.setUploadedAt(m.getUploadedAt());
        r.setEmployeeId(employeeId); // avoid lazy access
        return r;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FileMetaResponse> upload(
            @PathVariable String employeeId,
            @RequestPart("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        FileMeta meta = documentService.uploadDocument(employeeId, file, "ADMIN");
        return ResponseEntity.ok(toDto(meta, employeeId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FileMetaResponse>> getAll(@PathVariable String employeeId) {
        var list = documentService.getAllDocuments(employeeId)
                .stream().map(m -> toDto(m, employeeId)).toList();
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/{docId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FileMetaResponse> get(
            @PathVariable String employeeId, @PathVariable Long docId) {
        FileMeta meta = documentService.getDocument(employeeId, docId);
        return ResponseEntity.ok(toDto(meta, employeeId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> delete(@PathVariable String employeeId, @PathVariable Long docId) {
        documentService.deleteDocument(employeeId, docId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{docId}/download")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable String employeeId, @PathVariable Long docId) {
        return documentService.downloadDocument(employeeId, docId);
    }
}
