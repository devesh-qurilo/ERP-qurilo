/*package com.erp.project_service.service.impl;

import com.erp.project_service.entity.FileMeta;
import com.erp.project_service.entity.Project;
import com.erp.project_service.entity.Task;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.FileMetaMapper;
import com.erp.project_service.repository.FileMetaRepository;
import com.erp.project_service.repository.ProjectRepository;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.service.interfaces.FileService;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileMetaRepository repo;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectActivityService activityService;

    private final RestTemplate rest = new RestTemplate();

    @Value("${project.supabase.url}")
    private String supabaseUrl;

    @Value("${project.supabase.service-role-key}")
    private String supabaseServiceKey;

    @Value("${project.supabase.bucket}")
    private String defaultBucket;

    // PUBLIC URL base: https://<ref>.supabase.co/storage/v1/object/public/<bucket>/<path>
    @Value("${project.supabase.public-bucket-base:/storage/v1/object/public}")
    private String publicBase;

    private HttpHeaders supabaseHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", supabaseServiceKey);
        h.set("Authorization", "Bearer " + supabaseServiceKey);
        return h;
    }



    @Override
    @Transactional
    public com.erp.project_service.dto.file.FileMetaDto uploadTaskFile(Long taskId, MultipartFile file, String uploadedBy) {
        try {
            Task task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
            Project project = projectRepository.findById(task.getProjectId()).orElseThrow(() -> new NotFoundException("Project not found"));

            boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
            if (!isAdmin && (project.getAssignedEmployeeIds() == null || !project.getAssignedEmployeeIds().contains(uploadedBy))) {
                throw new RuntimeException("Forbidden: not assigned to project");
            }

            String safeFilename = sanitizeFilename(file.getOriginalFilename());
            String path = String.format("projects/%d/tasks/%d/%d-%s-%s",
                    project.getId(), taskId, Instant.now().toEpochMilli(), UUID.randomUUID().toString().substring(0,8), safeFilename);

            String bucket = defaultBucket;

            // ✅ FIX: URL encoding check karo
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
            log.info("Attempting file upload to: {}", uploadUrl);

            HttpHeaders headers = supabaseHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getContentType() == null ? "application/octet-stream" : file.getContentType()));

            byte[] body;
            try (InputStream is = file.getInputStream()) {
                body = StreamUtils.copyToByteArray(is);
            } catch (Exception e) {
                log.error("Failed to read file bytes for task {}: {}", taskId, e.getMessage());
                return createFileMetaWithoutUpload(taskId, project.getId(), safeFilename, file, uploadedBy);
            }

            HttpEntity<byte[]> req = new HttpEntity<>(body, headers);

            // ✅ FIX: Better error handling with detailed logging
            try {
                log.info("Sending POST request to Supabase...");
                ResponseEntity<String> resp = rest.exchange(uploadUrl, HttpMethod.POST, req, String.class);

                if (resp.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ File upload successful for task {}: {}", taskId, resp.getStatusCode());
                } else {
                    log.warn("POST failed ({}), trying PUT...", resp.getStatusCode());
                    resp = rest.exchange(uploadUrl, HttpMethod.PUT, req, String.class);

                    if (!resp.getStatusCode().is2xxSuccessful()) {
                        log.error("❌ Supabase upload failed for task {}: {} - Body: {}", taskId, resp.getStatusCode(), resp.getBody());
                        return createFileMetaWithoutUpload(taskId, project.getId(), safeFilename, file, uploadedBy);
                    } else {
                        log.info("✅ File upload successful via PUT for task {}", taskId);
                    }
                }
            } catch (Exception e) {
                log.error("❌ Network error during file upload for task {}: {}", taskId, e.getMessage());
                log.error("Exception details:", e); // Full stack trace
                return createFileMetaWithoutUpload(taskId, project.getId(), safeFilename, file, uploadedBy);
            }

            // ✅ FIX: Public URL generation check karo
            String publicUrl = supabaseUrl + publicBase + "/" + bucket + "/" + urlEncode(path);
            log.info("Generated public URL: {}", publicUrl);

            FileMeta fm = FileMeta.builder()
                    .taskId(taskId)
                    .projectId(project.getId())
                    .filename(safeFilename)
                    .bucket(bucket)
                    .path(path)
                    .url(publicUrl)
                    .mimeType(file.getContentType())
                    .size(file.getSize())
                    .uploadedBy(uploadedBy)
                    .build();

            FileMeta saved = repo.save(fm);
            activityService.record(project.getId(), uploadedBy, "TASK_FILE_UPLOADED", saved.getFilename());

            log.info("✅ File meta saved successfully for task {}", taskId);
            return FileMetaMapper.toDto(saved);

        } catch (Exception e) {
            log.error("❌ File upload failed for task {}: {}", taskId, e.getMessage());
            log.error("Full exception:", e);
            return null;
        }
    }

    // ✅ NEW: Supabase connection test method
    @EventListener(ApplicationReadyEvent.class)
    public void testSupabaseConnectionOnStartup() {
        log.info("🔍 Testing Supabase connection...");

        try {
            String testUrl = supabaseUrl + "/storage/v1/health";
            HttpEntity<Void> request = new HttpEntity<>(supabaseHeaders());

            ResponseEntity<String> response = rest.exchange(testUrl, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Supabase connection successful - Status: {}", response.getStatusCode());
            } else {
                log.warn("⚠️ Supabase connection issue - Status: {} - Body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Supabase connection failed: {}", e.getMessage());
        }
    }

    // ✅ FIX: supabaseHeaders method mein bhi logging add karo
//    private HttpHeaders supabaseHeaders() {
//        HttpHeaders h = new HttpHeaders();
//        h.set("apikey", supabaseServiceKey);
//        h.set("Authorization", "Bearer " + supabaseServiceKey);
//
//        // ✅ Debug logging
//        log.debug("Supabase Headers - API Key present: {}, URL: {}",
//                supabaseServiceKey != null && !supabaseServiceKey.isEmpty(),
//                supabaseUrl);
//
//        return h;
//    }h
    // ✅ YE METHOD ADD KARO FileServiceImpl CLASS KE ANDAR
    private com.erp.project_service.dto.file.FileMetaDto createFileMetaWithoutUpload(Long taskId, Long projectId, String filename, MultipartFile file, String uploadedBy) {
        try {
            FileMeta fm = FileMeta.builder()
                    .taskId(taskId)
                    .projectId(projectId)
                    .filename(filename + " (upload failed)")
                    .bucket(defaultBucket)
                    .path("failed-upload/" + filename)
                    .url("") // Empty URL since upload failed
                    .mimeType(file.getContentType())
                    .size(file.getSize())
                    .uploadedBy(uploadedBy)
                    .build();

            FileMeta saved = repo.save(fm);
            log.info("Created file meta record for failed upload: {}", filename);
            return FileMetaMapper.toDto(saved);
        } catch (Exception ex) {
            log.error("Failed to create file meta record: {}", ex.getMessage());
            return null;
        }
    }



    // ---------- Upload Project File ----------
    @Override
    @Transactional
    public com.erp.project_service.dto.file.FileMetaDto uploadProjectFile(Long projectId, MultipartFile file, String uploadedBy) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        // RBAC: uploader must be admin OR assigned to project
        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
        if (!isAdmin && (project.getAssignedEmployeeIds() == null || !project.getAssignedEmployeeIds().contains(uploadedBy))) {
            throw new RuntimeException("Forbidden: not assigned to project");
        }

        String bucket = defaultBucket;
        String safeFilename = sanitizeFilename(file.getOriginalFilename());

        // Create path WITHOUT URL encoding for upload
        String path = String.format("projects/%d/%d-%s-%s",
                projectId, Instant.now().toEpochMilli(), UUID.randomUUID().toString().substring(0,8), safeFilename);

        // ✅ FIX: Use raw path for upload URL (no URL encoding)
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = supabaseHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getContentType() == null ? "application/octet-stream" : file.getContentType()));

        byte[] body;
        try (InputStream is = file.getInputStream()) {
            body = StreamUtils.copyToByteArray(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }

        HttpEntity<byte[]> req = new HttpEntity<>(body, headers);

        // Try POST first, then PUT
        ResponseEntity<String> resp = rest.exchange(uploadUrl, HttpMethod.POST, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            resp = rest.exchange(uploadUrl, HttpMethod.PUT, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase upload failed: " + resp.getStatusCode() + " body=" + resp.getBody());
            }
        }

        // ✅ FIX: Use URL encoding ONLY for public URL (not for upload)
        String publicUrl = supabaseUrl + publicBase + "/" + bucket + "/" + urlEncode(path);

        FileMeta fm = FileMeta.builder()
                .projectId(projectId)
                .filename(safeFilename)
                .bucket(bucket)
                .path(path) // Store raw path
                .url(publicUrl)
                .mimeType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(uploadedBy)
                .build();

        FileMeta saved = repo.save(fm);
        activityService.record(projectId, uploadedBy, "PROJECT_FILE_UPLOADED", saved.getFilename());
        return FileMetaMapper.toDto(saved);
    }



    // ---------- Upload Task File ----------
   /* @Override
    @Transactional
    public com.erp.project_service.dto.file.FileMetaDto uploadTaskFile(Long taskId, MultipartFile file, String uploadedBy) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
        // project check
        Project project = projectRepository.findById(task.getProjectId()).orElseThrow(() -> new NotFoundException("Project not found"));

        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
        if (!isAdmin && (project.getAssignedEmployeeIds() == null || !project.getAssignedEmployeeIds().contains(uploadedBy))) {
            throw new RuntimeException("Forbidden: not assigned to project");
        }

        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        String path = String.format("projects/%d/tasks/%d/%d-%s-%s",
                project.getId(), taskId, Instant.now().toEpochMilli(), UUID.randomUUID().toString().substring(0,8), safeFilename);

        String bucket = defaultBucket;

        // ✅ FIX: Remove URL encoding from upload URL
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = supabaseHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getContentType() == null ? "application/octet-stream" : file.getContentType()));
        byte[] body;
        try (InputStream is = file.getInputStream()) {
            body = StreamUtils.copyToByteArray(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }

        HttpEntity<byte[]> req = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = rest.exchange(uploadUrl, HttpMethod.POST, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            resp = rest.exchange(uploadUrl, HttpMethod.PUT, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Supabase upload failed: " + resp.getStatusCode());
            }
        }

        // ✅ FIX: Use URL encoding ONLY for public URL
        String publicUrl = supabaseUrl + publicBase + "/" + bucket + "/" + urlEncode(path);

        FileMeta fm = FileMeta.builder()
                .taskId(taskId)
                .projectId(project.getId())
                .filename(safeFilename)
                .bucket(bucket)
                .path(path)  // Store raw path
                .url(publicUrl)
                .mimeType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(uploadedBy)
                .build();

        FileMeta saved = repo.save(fm);
        activityService.record(project.getId(), uploadedBy, "TASK_FILE_UPLOADED", saved.getFilename());
        return FileMetaMapper.toDto(saved);
    }

    */
/*

    // ---------- List ----------
    @Override
    public List<com.erp.project_service.dto.file.FileMetaDto> listProjectFiles(Long projectId) {
        return repo.findByProjectId(projectId).stream().map(FileMetaMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<com.erp.project_service.dto.file.FileMetaDto> listTaskFiles(Long taskId) {
        return repo.findByTaskId(taskId).stream().map(FileMetaMapper::toDto).collect(Collectors.toList());
    }

    // ---------- Delete ----------
    @Override
    @Transactional
    public void deleteFile(Long fileId, String actor) {
        FileMeta f = repo.findById(fileId).orElseThrow(() -> new NotFoundException("File not found"));

        // RBAC: admin or uploader or project assigned
        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
        if (!isAdmin && !actor.equals(f.getUploadedBy())) {
            Project p = null;
            if (f.getProjectId() != null) p = projectRepository.findById(f.getProjectId()).orElse(null);
            if (p == null || p.getAssignedEmployeeIds() == null || !p.getAssignedEmployeeIds().contains(actor)) {
                throw new RuntimeException("Forbidden to delete");
            }
        }

        // ✅ FIX: Store file info before deletion
        String bucket = f.getBucket();
        String path = f.getPath();
        String filename = f.getFilename();

        // ✅ FIX: Delete record first
        repo.deleteById(fileId);
        System.out.println("Deleted file record: " + fileId + " - " + filename);

        // Record activity if project exists
        if (f.getProjectId() != null) {
            activityService.record(f.getProjectId(), actor, "FILE_DELETED", filename);
        }

        // ✅ FIX: Delete from Supabase storage with better error handling
        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
        HttpEntity<Void> req = new HttpEntity<>(supabaseHeaders());
        try {
            ResponseEntity<String> resp = rest.exchange(deleteUrl, HttpMethod.DELETE, req, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                System.out.println("Successfully deleted file from Supabase: " + path);
            } else {
                System.err.println("Failed to delete from Supabase: " + resp.getStatusCode() + " - " + resp.getBody());
            }
        } catch (Exception ex) {
            System.err.println("Exception deleting from Supabase: " + ex.getMessage());
            // Don't rethrow - file record is already deleted
        }
    }

    // ---------- Download ----------
    // ---------- Download ----------
    // ---------- Download ----------
    @Override
    public byte[] downloadFile(Long fileId) {
        FileMeta f = repo.findById(fileId).orElseThrow(() -> new NotFoundException("File not found"));

        String downloadUrl = supabaseUrl + "/storage/v1/object/" + f.getBucket() + "/" + f.getPath();

        try {
            HttpHeaders headers = supabaseHeaders();
            // Do not set Content-Type on a GET; optionally set Accept if you want
            headers.setAccept(List.of(MediaType.ALL));

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = rest.exchange(
                    downloadUrl, HttpMethod.GET, requestEntity, byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Download failed: " + response.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    // ---------- Helpers ----------
    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
    }
    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}


 */

package com.erp.project_service.service.impl;

import com.erp.project_service.dto.file.FileMetaDto;
import com.erp.project_service.entity.FileMeta;
import com.erp.project_service.entity.Project;
import com.erp.project_service.entity.Task;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.mapper.FileMetaMapper;
import com.erp.project_service.repository.FileMetaRepository;
import com.erp.project_service.repository.ProjectRepository;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.service.interfaces.FileService;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileMetaRepository repo;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectActivityService activityService;

    private final RestTemplate rest = new RestTemplate();

    @Value("${project.supabase.url}")
    private String supabaseUrl;

    @Value("${project.supabase.service-role-key}")
    private String supabaseServiceKey;

    @Value("${project.supabase.bucket}")
    private String defaultBucket;

    /** public base (kept default same as before) */
    @Value("${project.supabase.public-bucket-base:/storage/v1/object/public}")
    private String publicBase;

    private HttpHeaders supabaseHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("apikey", supabaseServiceKey);
        h.set("Authorization", "Bearer " + supabaseServiceKey);
        return h;
    }

    // -------------------- Upload Task File --------------------
    @Override
    @Transactional
    public com.erp.project_service.dto.file.FileMetaDto uploadTaskFile(Long taskId, MultipartFile file, String uploadedBy) {
        try {
            Task task = taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
            Project project = projectRepository.findById(task.getProjectId()).orElseThrow(() -> new NotFoundException("Project not found"));

            boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
            if (!isAdmin && (project.getAssignedEmployeeIds() == null || !project.getAssignedEmployeeIds().contains(uploadedBy))) {
                throw new RuntimeException("Forbidden: not assigned to project");
            }

            // Sanitize filename
            String safeFilename = sanitizeFilename(file.getOriginalFilename());

            // Build upload path (raw path, NOT URL-encoded)
            String path = String.format("projects/%d/tasks/%d/%d-%s-%s",
                    project.getId(), taskId, Instant.now().toEpochMilli(), UUID.randomUUID().toString().substring(0,8), safeFilename);

            String bucket = defaultBucket;

            // Validate supabaseUrl host resolution BEFORE trying upload (gives clearer error)
            if (!isHostResolvable(supabaseUrl)) {
                log.error("Supabase host not resolvable: {}. Aborting upload and creating failed file meta record.", supabaseUrl);
                return createFileMetaWithoutUpload(taskId, project.getId(), safeFilename, file, uploadedBy);
            }

            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
            log.info("Attempting file upload to: {}", uploadUrl);

            HttpHeaders headers = supabaseHeaders();
            headers.setContentType(MediaType.parseMediaType(file.getContentType() == null ? "application/octet-stream" : file.getContentType()));

            byte[] body;
            try (InputStream is = file.getInputStream()) {
                body = StreamUtils.copyToByteArray(is);
            } catch (Exception e) {
                log.error("Failed to read file bytes for task {}: {}", taskId, e.getMessage());
                return createFileMetaWithoutUpload(taskId, project.getId(), safeFilename, file, uploadedBy);
            }

            HttpEntity<byte[]> req = new HttpEntity<>(body, headers);

            // Try POST then fallback to PUT
            try {
                log.info("Sending POST request to Supabase...");
                ResponseEntity<String> resp = rest.exchange(uploadUrl, HttpMethod.POST, req, String.class);

                if (!resp.getStatusCode().is2xxSuccessful()) {
                    log.warn("POST returned {}. Trying PUT...", resp.getStatusCode());
                    resp = rest.exchange(uploadUrl, HttpMethod.PUT, req, String.class);
                    if (!resp.getStatusCode().is2xxSuccessful()) {
                        log.error("Supabase upload failed (PUT): {} - Body: {}", resp.getStatusCode(), resp.getBody());
                        return createFileMetaWithoutUpload(taskId, project.getId(), safeFilename, file, uploadedBy);
                    } else {
                        log.info("File uploaded via PUT for task {}", taskId);
                    }
                } else {
                    log.info("File uploaded via POST for task {}", taskId);
                }

            } catch (Exception e) {
                log.error("Network error during file upload for task {}: {}", taskId, e.getMessage());
                log.debug("Full exception:", e);
                return createFileMetaWithoutUpload(taskId, project.getId(), safeFilename, file, uploadedBy);
            }

            String publicUrl = supabaseUrl + publicBase + "/" + bucket + "/" + urlEncode(path);
            FileMeta fm = FileMeta.builder()
                    .taskId(taskId)
                    .projectId(project.getId())
                    .filename(safeFilename)
                    .bucket(bucket)
                    .path(path)
                    .url(publicUrl)
                    .mimeType(file.getContentType())
                    .size(file.getSize())
                    .uploadedBy(uploadedBy)
                    .build();

            FileMeta saved = repo.save(fm);
            activityService.record(project.getId(), uploadedBy, "TASK_FILE_UPLOADED", saved.getFilename());
            log.info("✅ File meta saved successfully for task {}", taskId);
            return FileMetaMapper.toDto(saved);

        } catch (Exception e) {
            log.error("❌ File upload failed for task {}: {}", taskId, e.getMessage());
            log.debug("Full exception:", e);
            return null;
        }
    }

    // ---------- List ----------
    @Override
    public List<com.erp.project_service.dto.file.FileMetaDto> listProjectFiles(Long projectId) {
        return repo.findByProjectId(projectId).stream().map(FileMetaMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<com.erp.project_service.dto.file.FileMetaDto> listTaskFiles(Long taskId) {
        return repo.findByTaskId(taskId).stream().map(FileMetaMapper::toDto).collect(Collectors.toList());
    }


    // ---------- Helper: check host resolvability ----------
    private boolean isHostResolvable(String baseUrl) {
        try {
            if (baseUrl == null || baseUrl.isBlank()) return false;
            String host = baseUrl.replaceFirst("^https?://", "").split("/")[0];
            InetAddress.getByName(host);
            log.debug("Host resolvable: {}", host);
            return true;
        } catch (Exception ex) {
            log.warn("Host not resolvable for url {}: {}", baseUrl, ex.getMessage());
            return false;
        }
    }

    // ---------- Create file meta record when upload failed ----------
    private com.erp.project_service.dto.file.FileMetaDto createFileMetaWithoutUpload(Long taskId, Long projectId, String filename, MultipartFile file, String uploadedBy) {
        try {
            FileMeta fm = FileMeta.builder()
                    .taskId(taskId)
                    .projectId(projectId)
                    .filename(filename + " (upload failed)")
                    .bucket(defaultBucket)
                    .path("failed-upload/" + filename)
                    .url("")
                    .mimeType(file.getContentType())
                    .size(file.getSize())
                    .uploadedBy(uploadedBy)
                    .build();

            FileMeta saved = repo.save(fm);
            log.info("Created file meta record for failed upload: {}", filename);
            return FileMetaMapper.toDto(saved);
        } catch (Exception ex) {
            log.error("Failed to create file meta record: {}", ex.getMessage());
            return null;
        }
    }

    // ---------- Upload Project File (same pattern as task file) ----------
    @Override
    @Transactional
    public com.erp.project_service.dto.file.FileMetaDto uploadProjectFile(Long projectId, MultipartFile file, String uploadedBy) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
        if (!isAdmin && (project.getAssignedEmployeeIds() == null || !project.getAssignedEmployeeIds().contains(uploadedBy))) {
            throw new RuntimeException("Forbidden: not assigned to project");
        }

        String bucket = defaultBucket;
        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        String path = String.format("projects/%d/%d-%s-%s",
                projectId, Instant.now().toEpochMilli(), UUID.randomUUID().toString().substring(0,8), safeFilename);

        if (!isHostResolvable(supabaseUrl)) {
            log.error("Supabase host not resolvable: {}. Aborting upload and creating failed file meta record.", supabaseUrl);
            return createFileMetaWithoutUpload(null, projectId, safeFilename, file, uploadedBy);
        }

        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
        HttpHeaders headers = supabaseHeaders();
        headers.setContentType(MediaType.parseMediaType(file.getContentType() == null ? "application/octet-stream" : file.getContentType()));

        byte[] body;
        try (InputStream is = file.getInputStream()) {
            body = StreamUtils.copyToByteArray(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }

        HttpEntity<byte[]> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> resp = rest.exchange(uploadUrl, HttpMethod.POST, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                resp = rest.exchange(uploadUrl, HttpMethod.PUT, req, String.class);
                if (!resp.getStatusCode().is2xxSuccessful()) {
                    log.error("Supabase upload failed for project file: {} - {}", resp.getStatusCode(), resp.getBody());
                    return createFileMetaWithoutUpload(null, projectId, safeFilename, file, uploadedBy);
                }
            }
        } catch (Exception e) {
            log.error("Network error during project file upload: {}", e.getMessage());
            return createFileMetaWithoutUpload(null, projectId, safeFilename, file, uploadedBy);
        }

        String publicUrl = supabaseUrl + publicBase + "/" + bucket + "/" + urlEncode(path);

        FileMeta fm = FileMeta.builder()
                .projectId(projectId)
                .filename(safeFilename)
                .bucket(bucket)
                .path(path)
                .url(publicUrl)
                .mimeType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(uploadedBy)
                .build();

        FileMeta saved = repo.save(fm);
        activityService.record(projectId, uploadedBy, "PROJECT_FILE_UPLOADED", saved.getFilename());
        return FileMetaMapper.toDto(saved);
    }

    // ---------- Delete ----------
    @Override
    @Transactional
    public void deleteFile(Long fileId, String actor) {
        FileMeta f = repo.findById(fileId).orElseThrow(() -> new NotFoundException("File not found"));

        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
        if (!isAdmin && !actor.equals(f.getUploadedBy())) {
            Project p = null;
            if (f.getProjectId() != null) p = projectRepository.findById(f.getProjectId()).orElse(null);
            if (p == null || p.getAssignedEmployeeIds() == null || !p.getAssignedEmployeeIds().contains(actor)) {
                throw new RuntimeException("Forbidden to delete");
            }
        }

        String bucket = f.getBucket();
        String path = f.getPath();
        String filename = f.getFilename();

        // delete DB record first
        repo.deleteById(fileId);
        log.info("Deleted file record: {} - {}", fileId, filename);

        if (f.getProjectId() != null) {
            activityService.record(f.getProjectId(), actor, "FILE_DELETED", filename);
        }

        // Attempt delete from Supabase, but don't fail if network issue
        if (!isHostResolvable(supabaseUrl)) {
            log.warn("Supabase host not resolvable; skipping remote delete for path {}", path);
            return;
        }

        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;
        HttpEntity<Void> req = new HttpEntity<>(supabaseHeaders());
        try {
            ResponseEntity<String> resp = rest.exchange(deleteUrl, HttpMethod.DELETE, req, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted file from Supabase: {}", path);
            } else {
                log.warn("Failed to delete from Supabase: {} - {}", resp.getStatusCode(), resp.getBody());
            }
        } catch (Exception ex) {
            log.warn("Exception deleting from Supabase: {}", ex.getMessage());
        }
    }

    // ---------- Download ----------
    @Override
    public byte[] downloadFile(Long fileId) {
        FileMeta f = repo.findById(fileId).orElseThrow(() -> new NotFoundException("File not found"));
        String downloadUrl = supabaseUrl + "/storage/v1/object/" + f.getBucket() + "/" + f.getPath();

        if (!isHostResolvable(supabaseUrl)) {
            throw new RuntimeException("Supabase host not resolvable");
        }

        try {
            HttpHeaders headers = supabaseHeaders();
            headers.setAccept(List.of(MediaType.ALL));
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = rest.exchange(downloadUrl, HttpMethod.GET, requestEntity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new RuntimeException("Download failed: " + response.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
        }
    }

    // ---------- Helpers ----------
    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._\\- ]", "_");
    }
    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // Startup health check logging (non-blocking)
    @EventListener(ApplicationReadyEvent.class)
    public void testSupabaseConnectionOnStartup() {
        log.info("🔍 Testing Supabase connection...");
        try {
            if (!isHostResolvable(supabaseUrl)) {
                log.error("Supabase host {} not resolvable on startup.", supabaseUrl);
                return;
            }
            String testUrl = supabaseUrl + "/storage/v1/health";
            HttpEntity<Void> request = new HttpEntity<>(supabaseHeaders());
            ResponseEntity<String> response = rest.exchange(testUrl, HttpMethod.GET, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Supabase connection successful - Status: {}", response.getStatusCode());
            } else {
                log.warn("⚠️ Supabase connection issue - Status: {} - Body: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Supabase connection failed on startup: {}", e.getMessage());
            log.debug("Full exception:", e);
        }
    }
}
