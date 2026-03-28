package com.erp.project_service.controller;

import com.erp.project_service.dto.file.FileMetaDto;
import com.erp.project_service.service.interfaces.FileService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService svc;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/projects/{projectId}")
    public ResponseEntity<FileMetaDto> uploadProjectFile(@PathVariable Long projectId,
                                                         @RequestPart("file") MultipartFile file) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(svc.uploadProjectFile(projectId, file, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @PostMapping("/tasks/{taskId}")
    public ResponseEntity<FileMetaDto> uploadTaskFile(@PathVariable Long taskId,
                                                      @RequestPart("file") MultipartFile file) {
        String actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(svc.uploadTaskFile(taskId, file, actor));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<List<FileMetaDto>> listProjectFiles(@PathVariable Long projectId) {
        return ResponseEntity.ok(svc.listProjectFiles(projectId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<List<FileMetaDto>> listTaskFiles(@PathVariable Long taskId) {
        return ResponseEntity.ok(svc.listTaskFiles(taskId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/{fileId}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long fileId) {
        byte[] bytes = svc.downloadFile(fileId);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(bytes.length);
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename("file").build());
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> delete(@PathVariable Long fileId) {
        String actor = SecurityUtils.getCurrentUserId();
        svc.deleteFile(fileId, actor);
        return ResponseEntity.noContent().build();
    }
}
