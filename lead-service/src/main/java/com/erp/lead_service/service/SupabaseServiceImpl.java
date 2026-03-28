package com.erp.lead_service.service;

import com.erp.lead_service.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseServiceImpl implements SupabaseService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String supabaseServiceKey;

    @Value("${supabase.bucket:public}")
    private String bucketName;

    private final RestTemplate restTemplate;

    @Override
    public String uploadFile(MultipartFile file, Long dealId) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // Generate unique filename with folder structure
            String objectPath = "deal-documents/" + dealId + "/" + UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

            // Correct Supabase Storage API endpoint
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath;

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseServiceKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Prepare file data
            byte[] fileBytes = StreamUtils.copyToByteArray(file.getInputStream());
            ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    // Return only the filename part, not the full path
                    return objectPath.substring(objectPath.lastIndexOf("/") + 1);
                }
            };

            // Create form data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Uploading file to Supabase. Bucket: {}, Object path: {}, Size: {} bytes",
                    bucketName, objectPath, fileBytes.length);

            // Make the request
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // Return public URL
                String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + objectPath;
                log.info("File uploaded successfully: {}", publicUrl);
                return publicUrl;
            } else {
                log.error("Supabase upload failed: status {} - {}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("Supabase upload failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error uploading to Supabase: {}", e.getMessage(), e);
            throw new RuntimeException("Error uploading file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFileByUrl(String url) {
        try {
            // Extract object path from URL
            String objectPath = extractObjectPathFromUrl(url);

            if (objectPath != null) {
                // Include bucket name in the delete URL
                String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath;

                log.info("Attempting to delete file. Bucket: {}, Object path: {}, Full URL: {}",
                        bucketName, objectPath, deleteUrl);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + supabaseServiceKey);

                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        deleteUrl,
                        HttpMethod.DELETE,
                        entity,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("File deleted successfully: {}", url);
                } else {
                    log.error("Failed to delete file: Status {} - {}", response.getStatusCode(), response.getBody());
                    throw new RuntimeException("Delete failed with status: " + response.getStatusCode());
                }
            } else {
                throw new RuntimeException("Could not extract object path from URL: " + url);
            }
        } catch (Exception e) {
            log.error("Error deleting file from Supabase: {}", e.getMessage(), e);
            throw new RuntimeException("Error deleting file: " + e.getMessage(), e);
        }
    }

    private String extractObjectPathFromUrl(String url) {
        try {
            log.info("Extracting object path from URL: {}", url);

            // Method 1: Remove the public URL prefix
            String publicUrlPrefix = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/";
            if (url.startsWith(publicUrlPrefix)) {
                String objectPath = url.substring(publicUrlPrefix.length());
                log.info("Extracted object path (Method 1): {}", objectPath);
                return objectPath;
            }

            // Method 2: Look for pattern after "object/public/"
            int publicIndex = url.indexOf("/object/public/");
            if (publicIndex != -1) {
                String afterPublic = url.substring(publicIndex + "/object/public/".length());
                if (afterPublic.startsWith(bucketName + "/")) {
                    String objectPath = afterPublic.substring(bucketName.length() + 1);
                    log.info("Extracted object path (Method 2): {}", objectPath);
                    return objectPath;
                }
            }

            // Method 3: Look for pattern after "object/" (for signed URLs)
            int objectIndex = url.indexOf("/object/");
            if (objectIndex != -1) {
                String afterObject = url.substring(objectIndex + "/object/".length());
                if (afterObject.startsWith(bucketName + "/")) {
                    String objectPath = afterObject.substring(bucketName.length() + 1);
                    log.info("Extracted object path (Method 3): {}", objectPath);
                    return objectPath;
                }
                // If bucket name is not included, try to extract directly
                String objectPath = afterObject;
                log.info("Extracted object path (Method 3 alt): {}", objectPath);
                return objectPath;
            }

            // Method 4: If URL is just the object path (shouldn't happen but handle it)
            if (!url.contains("://") && url.contains("/")) {
                log.info("URL appears to be just an object path: {}", url);
                return url;
            }

            log.warn("Could not extract object path from URL: {}", url);
            return null;

        } catch (Exception e) {
            log.error("Failed to extract object path from URL: {}", url, e);
            return null;
        }
    }

    // Alternative method that accepts direct object path (more reliable)
    public void deleteFileByObjectPath(String objectPath) {
        try {
            if (objectPath == null || objectPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Object path cannot be null or empty");
            }

            // Ensure object path doesn't start with /
            if (objectPath.startsWith("/")) {
                objectPath = objectPath.substring(1);
            }

            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + objectPath;

            log.info("Deleting file by object path. Bucket: {}, Path: {}, URL: {}",
                    bucketName, objectPath, deleteUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseServiceKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("File deleted successfully by object path: {}", objectPath);
            } else {
                log.error("Failed to delete file by object path: Status {} - {}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Delete failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error deleting file by object path: {}", e.getMessage(), e);
            throw new RuntimeException("Error deleting file: " + e.getMessage(), e);
        }
    }
}