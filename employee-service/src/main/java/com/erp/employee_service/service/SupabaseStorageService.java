//package com.erp.employee_service.service;
//
//import com.erp.employee_service.entity.FileMeta;
//import jakarta.annotation.PostConstruct;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class SupabaseStorageService {
//
//    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);
//
//    private final WebClient webClient;
//
//    @Value("${supabase.url}")
//    private String supabaseUrl;
//
//    @Value("${supabase.service-role-key}")
//    private String supabaseKey;
//
//    @Value("${supabase.bucket}")
//    private String bucket;
//
//    @Value("${supabase.award-bucket:awards}") // Default to 'awards' if not configured
//    private String awardBucket;
//
//    public SupabaseStorageService(@Qualifier("supabaseWebClient") WebClient webClient) {
//        this.webClient = webClient;
//    }
//
//    @PostConstruct
//    public void init() {
//        // normalize supabaseUrl (no trailing slash)
//        if (supabaseUrl != null && supabaseUrl.endsWith("/")) {
//            supabaseUrl = supabaseUrl.substring(0, supabaseUrl.length() - 1);
//        }
//        log.info("SupabaseStorageService initialized with url='{}' bucket='{}' awardBucket='{}'", supabaseUrl, bucket, awardBucket);
//        if (supabaseUrl == null || supabaseUrl.isBlank()) {
//            log.warn("supabase.url is not configured!");
//        }
//        if (supabaseKey == null || supabaseKey.isBlank()) {
//            log.warn("supabase.service-role-key is not configured! Upload/delete may fail without service role key.");
//        }
//    }
//
//    /**
//     * Uploads file to Supabase storage under given folder (folder may include subfolders).
//     * Returns a FileMeta (not yet persisted) containing path and public URL (encoded).
//     *
//     * - folder: e.g. "profile-pics/EMP001" or "profile-pics/EMP001/avatar.jpg"
//     *   If folder doesn't end with filename, a UUID-prefixed filename will be used.
//     */
//
//    public FileMeta uploadFile(MultipartFile file, String folder, String uploadedBy) {
//        try {
//            if (file == null || file.isEmpty()) {
//                throw new IllegalArgumentException("file is empty");
//            }
//
//            String origRaw = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
//            String orig = sanitizeFilename(origRaw); // आपकी existing helper
//
//            String folderTrim = folder == null ? "" : folder.replaceAll("^/+", "").replaceAll("/+$", "");
//            String objectPath;
//            if (!folderTrim.isEmpty() && folderTrim.endsWith(orig)) {
//                objectPath = folderTrim;
//            } else {
//                String safeName = UUID.randomUUID() + "-" + orig;
//                objectPath = folderTrim.isEmpty() ? safeName : folderTrim + "/" + safeName;
//            }
//
//            // debug logs - very helpful
//            log.info("Preparing upload: orig='{}' objectPath='{}' contentType='{}' size={}",
//                    origRaw, objectPath, file.getContentType(), file.getSize());
//
//            // Use URI builder to avoid double-encoding issues
//            webClient.put()
//                    .uri(uriBuilder -> uriBuilder
//                            .path("/storage/v1/object/{bucket}/{path}")
//                            .build(bucket, objectPath))
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
//                    .header("apikey", supabaseKey)
//                    .header("x-upsert", "true") // optional: allow overwrite
//                    .contentType(MediaType.parseMediaType(Optional.ofNullable(file.getContentType())
//                            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
//                    .contentLength(file.getSize())
//                    .bodyValue(file.getBytes())
//                    .retrieve()
//                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
//                        log.error("Supabase upload returned {} -> {}", resp.statusCode(), body);
//                        return new RuntimeException("Supabase upload failed: " + resp.statusCode() + " - " + body);
//                    }))
//                    .bodyToMono(Void.class)
//                    .block();
//
//            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + encodePathPreserveSlashes(objectPath);
//
//            FileMeta meta = new FileMeta();
//            meta.setBucket(bucket);
//            meta.setPath(objectPath);
//            meta.setFilename(orig);
//            meta.setMime(file.getContentType());
//            meta.setSize(file.getSize());
//            meta.setUrl(publicUrl);
//            meta.setUploadedBy(uploadedBy);
//            meta.setUploadedAt(LocalDateTime.now());
//
//            log.info("Supabase upload success: {}", publicUrl);
//            return meta;
//        } catch (Exception ex) {
//            log.error("Supabase upload exception", ex);
//            throw new RuntimeException("Supabase upload failed: " + ex.getMessage(), ex);
//        }
//    }
//
//
//    /**
//     * Download raw bytes for object path (path = object key stored in FileMeta.path).
//     */
//    public byte[] downloadFile(String path) {
//        try {
//            if (path == null || path.isBlank()) throw new IllegalArgumentException("path required");
//            String encodedPath = encodePathPreserveSlashes(path);
//            return webClient.get()
//                    .uri("/storage/v1/object/{bucket}/{path}", bucket, encodedPath)
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
//                    .header("apikey", supabaseKey)
//                    .accept(MediaType.APPLICATION_OCTET_STREAM)
//                    .retrieve()
//                    .bodyToMono(byte[].class)
//                    .block();
//        } catch (Exception e) {
//            log.error("Supabase download failed for path: {}", path, e);
//            throw new RuntimeException("Supabase download failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Delete single file by object path (path as stored in FileMeta.path).
//     * This method swallows errors and logs them.
//     */
//    public void deleteFile(String path) {
//        try {
//            if (path == null || path.isBlank()) return;
//            String encodedPath = encodePathPreserveSlashes(path);
//
//            webClient.delete()
//                    .uri("/storage/v1/object/{bucket}/{path}", bucket, encodedPath)
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
//                    .header("apikey", supabaseKey)
//                    .retrieve()
//                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
//                        log.warn("Supabase delete returned {} -> {}", resp.statusCode(), body);
//                        return new RuntimeException("Supabase delete failed: " + resp.statusCode());
//                    }))
//                    .bodyToMono(Void.class)
//                    .block();
//
//            log.info("Deleted storage object: {}", path);
//        } catch (Exception e) {
//            log.warn("Failed to delete storage object {}: {}", path, e.getMessage());
//        }
//    }
//
//    /**
//     * List files under a prefix (folder-like). Returns list of object names (paths).
//     * Uses Supabase storage list endpoint: POST /storage/v1/object/list/{bucket}
//     */
//    @SuppressWarnings("unchecked")
//    public List<String> listFiles(String prefix) {
//        try {
//            Map<String, Object> body = new HashMap<>();
//            body.put("prefix", prefix == null ? "" : prefix);
//            body.put("limit", 1000);
//
//            List<Map> res = webClient.post()
//                    .uri("/storage/v1/object/list/{bucket}", bucket)
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
//                    .header("apikey", supabaseKey)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .bodyValue(body)
//                    .retrieve()
//                    .bodyToMono(List.class)
//                    .block();
//
//            if (res == null || res.isEmpty()) return Collections.emptyList();
//
//            return res.stream()
//                    .filter(obj -> obj instanceof Map)
//                    .map(obj -> ((Map) obj).get("name"))
//                    .filter(Objects::nonNull)
//                    .map(Object::toString)
//                    .collect(Collectors.toList());
//        } catch (Exception ex) {
//            log.warn("Failed to list files for prefix {}: {}", prefix, ex.getMessage());
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * Delete all objects under a prefix (folder) by listing and deleting each object.
//     */
//    public void deleteFolder(String prefix) {
//        if (prefix == null) return;
//        List<String> paths = listFiles(prefix);
//        for (String p : paths) {
//            deleteFile(p);
//        }
//        log.info("deleteFolder completed for prefix={}, deleted {} objects", prefix, paths.size());
//    }
//
//    /**
//     * Uploads award icon to Supabase storage and returns the public URL (encoded).
//     * objectPath can be a full path including filename or just a path; filename will be sanitized.
//     */
//    public String uploadAwardIcon(MultipartFile file, String objectPath) {
//        try {
//            if (file == null || file.isEmpty()) {
//                throw new IllegalArgumentException("File is empty");
//            }
//
//            if (objectPath == null || objectPath.isBlank()) {
//                throw new IllegalArgumentException("Object path is required");
//            }
//
//            // Clean and sanitize the path: remove leading/trailing slashes, sanitize each segment
//            String cleanPath = objectPath.replaceAll("^/+", "").replaceAll("/+$", "");
//            String sanitizedPath = sanitizePath(cleanPath);
//
//            // Encode path for HTTP
//            String encodedPath = encodePathPreserveSlashes(sanitizedPath);
//
//            // Validate file size and type
//            validateAwardIconFile(file);
//
//            // Upload
//            webClient.put()
//                    .uri("/storage/v1/object/{bucket}/{path}", awardBucket, encodedPath)
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
//                    .header("apikey", supabaseKey)
//                    .contentType(MediaType.parseMediaType(Optional.ofNullable(file.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
//                    .contentLength(file.getSize())
//                    .bodyValue(file.getBytes())
//                    .retrieve()
//                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
//                        log.error("Supabase award upload returned {} -> {}", resp.statusCode(), body);
//                        return new RuntimeException("Supabase upload failed: " + resp.statusCode() + " - " + body);
//                    }))
//                    .bodyToMono(Void.class)
//                    .block();
//
//            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + awardBucket + "/" + encodedPath;
//            log.info("Supabase award icon upload success: {}", publicUrl);
//            return publicUrl;
//        } catch (Exception ex) {
//            log.error("Supabase award icon upload exception for path: {}", objectPath, ex);
//            throw new RuntimeException("Supabase award icon upload failed: " + ex.getMessage(), ex);
//        }
//    }
//
//    /**
//     * Download award icon as bytes
//     */
//    public byte[] downloadAwardIcon(String path) {
//        try {
//            if (path == null || path.isBlank()) throw new IllegalArgumentException("Path required");
//
//            String cleanPath = path.replaceAll("^/+", "").replaceAll("/+$", "");
//            String encodedPath = encodePathPreserveSlashes(cleanPath);
//
//            return webClient.get()
//                    .uri("/storage/v1/object/{bucket}/{path}", awardBucket, encodedPath)
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
//                    .header("apikey", supabaseKey)
//                    .accept(MediaType.APPLICATION_OCTET_STREAM)
//                    .retrieve()
//                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
//                        log.error("Supabase award download returned {} -> {}", resp.statusCode(), body);
//                        return new RuntimeException("Supabase download failed: " + resp.statusCode() + " - " + body);
//                    }))
//                    .bodyToMono(byte[].class)
//                    .block();
//        } catch (Exception e) {
//            log.error("Supabase award icon download failed for path: {}", path, e);
//            throw new RuntimeException("Supabase award icon download failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Validate award icon file constraints
//     */
//    private void validateAwardIconFile(MultipartFile file) {
//        long maxSize = 5 * 1024 * 1024;
//        if (file.getSize() > maxSize) {
//            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
//        }
//
//        String[] allowedTypes = {"image/jpeg", "image/png", "image/gif", "image/svg+xml", "image/webp"};
//        String contentType = file.getContentType();
//        boolean isValidType = false;
//
//        if (contentType != null) {
//            for (String allowedType : allowedTypes) {
//                if (contentType.equalsIgnoreCase(allowedType)) {
//                    isValidType = true;
//                    break;
//                }
//            }
//        }
//
//        if (!isValidType) {
//            throw new IllegalArgumentException("Invalid file type. Allowed types: JPEG, PNG, GIF, SVG, WEBP");
//        }
//    }
//
//    /**
//     * Helper: encode a full path but preserve slashes between segments.
//     * Ensures spaces become %20 (not +) and other unsafe chars are percent-encoded.
//     */
//    private String encodePathPreserveSlashes(String path) {
//        if (path == null) return "";
//        // encode each segment separately to preserve '/'
//        String[] segments = path.split("/");
//        return Arrays.stream(segments)
//                .map(seg -> {
//                    try {
//                        // URLEncoder.encode will encode space as +; convert + to %20
//                        return URLEncoder.encode(seg, StandardCharsets.UTF_8.name()).replace("+", "%20");
//                    } catch (Exception e) {
//                        // fallback: remove problematic chars
//                        return seg.replaceAll("[^A-Za-z0-9.\\-_%()\\[\\]]+", "");
//                    }
//                })
//                .collect(Collectors.joining("/"));
//    }
//
//    /**
//     * Helper: sanitize a filename (replace whitespace with '-', remove dangerous chars).
//     */
//    private String sanitizeFilename(String input) {
//        if (input == null) return "file";
//        // remove path separators if any (safety)
//        String name = input.replaceAll("[\\\\/]+", "");
//        // collapse whitespace -> dash
//        name = name.trim().replaceAll("\\s+", "-");
//        // remove any control or weird characters, allow letters, numbers, dot, dash, underscore, parentheses, brackets, percent
//        name = name.replaceAll("[^A-Za-z0-9.\\-_%()\\[\\]]+", "");
//        if (name.isEmpty()) return UUID.randomUUID().toString();
//        return name;
//    }
//
//    /**
//     * Helper: sanitize each segment of a path (folders + filename)
//     */
//    private String sanitizePath(String path) {
//        if (path == null || path.isBlank()) return path;
//        String[] parts = path.split("/");
//        for (int i = 0; i < parts.length; i++) {
//            parts[i] = sanitizeFilename(parts[i]);
//        }
//        return String.join("/", parts);
//    }
//}
package com.erp.employee_service.service;

import com.erp.employee_service.entity.FileMeta;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucket;

    @Value("${supabase.award-bucket:awards}") // Default to 'awards' if not configured
    private String awardBucket;

    public SupabaseStorageService(@Qualifier("supabaseWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @PostConstruct
    public void init() {
        // normalize supabaseUrl (no trailing slash)
        if (supabaseUrl != null && supabaseUrl.endsWith("/")) {
            supabaseUrl = supabaseUrl.substring(0, supabaseUrl.length() - 1);
        }
        log.info("SupabaseStorageService initialized with url='{}' bucket='{}' awardBucket='{}'", supabaseUrl, bucket, awardBucket);
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            log.warn("supabase.url is not configured!");
        }
        if (supabaseKey == null || supabaseKey.isBlank()) {
            log.warn("supabase.service-role-key is not configured! Upload/delete may fail without service role key.");
        }
    }

    /**
     * Specifically for profile pictures - ensures only one file exists per employee
     * by deleting old file before uploading new one with consistent naming
     */
    public FileMeta uploadProfilePicture(MultipartFile file, String employeeId, String uploadedBy) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("file is empty");
            }

            // Consistent path for profile picture - always same for this employee
            String folder = "profile-pics/" + employeeId;
            String objectPath = folder + "/profile-picture";

            log.info("Uploading profile picture for employee {} to path: {}", employeeId, objectPath);

            // Step 1: Delete existing profile picture if any
            try {
                deleteFile(objectPath);
                log.info("Deleted existing profile picture for employee {}", employeeId);
            } catch (Exception ex) {
                log.warn("No existing profile picture to delete for employee {}: {}", employeeId, ex.getMessage());
            }

            // Step 2: Upload new file with consistent path
            webClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/storage/v1/object/{bucket}/{path}")
                            .build(bucket, objectPath))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("x-upsert", "true") // This ensures overwrite
                    .contentType(MediaType.parseMediaType(Optional.ofNullable(file.getContentType())
                            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
                    .contentLength(file.getSize())
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                        log.error("Supabase profile upload returned {} -> {}", resp.statusCode(), body);
                        return new RuntimeException("Supabase profile upload failed: " + resp.statusCode() + " - " + body);
                    }))
                    .bodyToMono(Void.class)
                    .block();

            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + encodePathPreserveSlashes(objectPath);

            FileMeta meta = new FileMeta();
            meta.setBucket(bucket);
            meta.setPath(objectPath);
            meta.setFilename("profile-picture"); // Consistent filename
            meta.setMime(file.getContentType());
            meta.setSize(file.getSize());
            meta.setUrl(publicUrl);
            meta.setUploadedBy(uploadedBy);
            meta.setUploadedAt(LocalDateTime.now());

            log.info("Profile picture upload success for employee {}: {}", employeeId, publicUrl);
            return meta;
        } catch (Exception ex) {
            log.error("Profile picture upload exception for employee {}", employeeId, ex);
            throw new RuntimeException("Profile picture upload failed: " + ex.getMessage(), ex);
        }
    }

    public FileMeta uploadFile(MultipartFile file, String folder, String uploadedBy) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("file is empty");
            }

            String origRaw = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String orig = sanitizeFilename(origRaw); // आपकी existing helper

            String folderTrim = folder == null ? "" : folder.replaceAll("^/+", "").replaceAll("/+$", "");
            String objectPath;
            if (!folderTrim.isEmpty() && folderTrim.endsWith(orig)) {
                objectPath = folderTrim;
            } else {
                String safeName = UUID.randomUUID() + "-" + orig;
                objectPath = folderTrim.isEmpty() ? safeName : folderTrim + "/" + safeName;
            }

            // debug logs - very helpful
            log.info("Preparing upload: orig='{}' objectPath='{}' contentType='{}' size={}",
                    origRaw, objectPath, file.getContentType(), file.getSize());

            // Use URI builder to avoid double-encoding issues
            webClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/storage/v1/object/{bucket}/{path}")
                            .build(bucket, objectPath))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("x-upsert", "true") // optional: allow overwrite
                    .contentType(MediaType.parseMediaType(Optional.ofNullable(file.getContentType())
                            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
                    .contentLength(file.getSize())
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                        log.error("Supabase upload returned {} -> {}", resp.statusCode(), body);
                        return new RuntimeException("Supabase upload failed: " + resp.statusCode() + " - " + body);
                    }))
                    .bodyToMono(Void.class)
                    .block();

            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + encodePathPreserveSlashes(objectPath);

            FileMeta meta = new FileMeta();
            meta.setBucket(bucket);
            meta.setPath(objectPath);
            meta.setFilename(orig);
            meta.setMime(file.getContentType());
            meta.setSize(file.getSize());
            meta.setUrl(publicUrl);
            meta.setUploadedBy(uploadedBy);
            meta.setUploadedAt(LocalDateTime.now());

            log.info("Supabase upload success: {}", publicUrl);
            return meta;
        } catch (Exception ex) {
            log.error("Supabase upload exception", ex);
            throw new RuntimeException("Supabase upload failed: " + ex.getMessage(), ex);
        }
    }


    /**
     * Download raw bytes for object path (path = object key stored in FileMeta.path).
     */
    public byte[] downloadFile(String path) {
        try {
            if (path == null || path.isBlank()) throw new IllegalArgumentException("path required");
            String encodedPath = encodePathPreserveSlashes(path);
            return webClient.get()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, encodedPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.error("Supabase download failed for path: {}", path, e);
            throw new RuntimeException("Supabase download failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete single file by object path (path as stored in FileMeta.path).
     * This method swallows errors and logs them.
     */
    public void deleteFile(String path) {
        try {
            if (path == null || path.isBlank()) return;
            String encodedPath = encodePathPreserveSlashes(path);

            webClient.delete()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, encodedPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                        log.warn("Supabase delete returned {} -> {}", resp.statusCode(), body);
                        return new RuntimeException("Supabase delete failed: " + resp.statusCode());
                    }))
                    .bodyToMono(Void.class)
                    .block();

            log.info("Deleted storage object: {}", path);
        } catch (Exception e) {
            log.warn("Failed to delete storage object {}: {}", path, e.getMessage());
        }
    }

    /**
     * List files under a prefix (folder-like). Returns list of object names (paths).
     * Uses Supabase storage list endpoint: POST /storage/v1/object/list/{bucket}
     */
    @SuppressWarnings("unchecked")
    public List<String> listFiles(String prefix) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("prefix", prefix == null ? "" : prefix);
            body.put("limit", 1000);

            List<Map> res = webClient.post()
                    .uri("/storage/v1/object/list/{bucket}", bucket)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (res == null || res.isEmpty()) return Collections.emptyList();

            return res.stream()
                    .filter(obj -> obj instanceof Map)
                    .map(obj -> ((Map) obj).get("name"))
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.warn("Failed to list files for prefix {}: {}", prefix, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Delete all objects under a prefix (folder) by listing and deleting each object.
     */
    public void deleteFolder(String prefix) {
        if (prefix == null) return;
        List<String> paths = listFiles(prefix);
        for (String p : paths) {
            deleteFile(p);
        }
        log.info("deleteFolder completed for prefix={}, deleted {} objects", prefix, paths.size());
    }

    /**
     * Uploads award icon to Supabase storage and returns the public URL (encoded).
     * objectPath can be a full path including filename or just a path; filename will be sanitized.
     */
    public String uploadAwardIcon(MultipartFile file, String objectPath) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            if (objectPath == null || objectPath.isBlank()) {
                throw new IllegalArgumentException("Object path is required");
            }

            // Clean and sanitize the path: remove leading/trailing slashes, sanitize each segment
            String cleanPath = objectPath.replaceAll("^/+", "").replaceAll("/+$", "");
            String sanitizedPath = sanitizePath(cleanPath);

            // Encode path for HTTP
            String encodedPath = encodePathPreserveSlashes(sanitizedPath);

            // Validate file size and type
            validateAwardIconFile(file);

            // Upload
            webClient.put()
                    .uri("/storage/v1/object/{bucket}/{path}", awardBucket, encodedPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.parseMediaType(Optional.ofNullable(file.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
                    .contentLength(file.getSize())
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                        log.error("Supabase award upload returned {} -> {}", resp.statusCode(), body);
                        return new RuntimeException("Supabase upload failed: " + resp.statusCode() + " - " + body);
                    }))
                    .bodyToMono(Void.class)
                    .block();

            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + awardBucket + "/" + encodedPath;
            log.info("Supabase award icon upload success: {}", publicUrl);
            return publicUrl;
        } catch (Exception ex) {
            log.error("Supabase award icon upload exception for path: {}", objectPath, ex);
            throw new RuntimeException("Supabase award icon upload failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Download award icon as bytes
     */
    public byte[] downloadAwardIcon(String path) {
        try {
            if (path == null || path.isBlank()) throw new IllegalArgumentException("Path required");

            String cleanPath = path.replaceAll("^/+", "").replaceAll("/+$", "");
            String encodedPath = encodePathPreserveSlashes(cleanPath);

            return webClient.get()
                    .uri("/storage/v1/object/{bucket}/{path}", awardBucket, encodedPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).map(body -> {
                        log.error("Supabase award download returned {} -> {}", resp.statusCode(), body);
                        return new RuntimeException("Supabase download failed: " + resp.statusCode() + " - " + body);
                    }))
                    .bodyToMono(byte[].class)
                    .block();
        } catch (Exception e) {
            log.error("Supabase award icon download failed for path: {}", path, e);
            throw new RuntimeException("Supabase award icon download failed: " + e.getMessage(), e);
        }
    }

    /**
     * Validate award icon file constraints
     */
    private void validateAwardIconFile(MultipartFile file) {
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        String[] allowedTypes = {"image/jpeg", "image/png", "image/gif", "image/svg+xml", "image/webp"};
        String contentType = file.getContentType();
        boolean isValidType = false;

        if (contentType != null) {
            for (String allowedType : allowedTypes) {
                if (contentType.equalsIgnoreCase(allowedType)) {
                    isValidType = true;
                    break;
                }
            }
        }

        if (!isValidType) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: JPEG, PNG, GIF, SVG, WEBP");
        }
    }

    /**
     * Helper: encode a full path but preserve slashes between segments.
     * Ensures spaces become %20 (not +) and other unsafe chars are percent-encoded.
     */
    private String encodePathPreserveSlashes(String path) {
        if (path == null) return "";
        // encode each segment separately to preserve '/'
        String[] segments = path.split("/");
        return Arrays.stream(segments)
                .map(seg -> {
                    try {
                        // URLEncoder.encode will encode space as +; convert + to %20
                        return URLEncoder.encode(seg, StandardCharsets.UTF_8.name()).replace("+", "%20");
                    } catch (Exception e) {
                        // fallback: remove problematic chars
                        return seg.replaceAll("[^A-Za-z0-9.\\-_%()\\[\\]]+", "");
                    }
                })
                .collect(Collectors.joining("/"));
    }

    /**
     * Helper: sanitize a filename (replace whitespace with '-', remove dangerous chars).
     */
    private String sanitizeFilename(String input) {
        if (input == null) return "file";
        // remove path separators if any (safety)
        String name = input.replaceAll("[\\\\/]+", "");
        // collapse whitespace -> dash
        name = name.trim().replaceAll("\\s+", "-");
        // remove any control or weird characters, allow letters, numbers, dot, dash, underscore, parentheses, brackets, percent
        name = name.replaceAll("[^A-Za-z0-9.\\-_%()\\[\\]]+", "");
        if (name.isEmpty()) return UUID.randomUUID().toString();
        return name;
    }

    /**
     * Helper: sanitize each segment of a path (folders + filename)
     */
    private String sanitizePath(String path) {
        if (path == null || path.isBlank()) return path;
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = sanitizeFilename(parts[i]);
        }
        return String.join("/", parts);
    }
}