package com.erp.client_service.service.supabase.impl;

import com.erp.client_service.exception.FileStorageException;

import com.erp.client_service.service.supabase.SupabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class SupabaseServiceImpl implements SupabaseService {

    private final WebClient webClient;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucket;

    @Override
    public String uploadFile(MultipartFile file, String path, boolean isProfilePicture) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }

        // Profile picture: allow only JPG/JPEG
        if (isProfilePicture) {
            String mime = file.getContentType();
            if (mime == null || !(mime.equalsIgnoreCase("image/jpeg") || mime.equalsIgnoreCase("image/jpg"))) {
                throw new FileStorageException("Only JPG/JPEG images allowed for profile picture");
            }
        }

        // sanitize filename: replace spaces, remove problematic chars
        String original = file.getOriginalFilename();
        String base = original == null ? "file" : original;
        // replace spaces with underscore, remove characters except alnum, dot, dash, underscore
        String safe = base.trim().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9._-]", "");
        safe = safe.toLowerCase();

        String fullPath = String.format("%s/%d_%s", path, System.currentTimeMillis(), safe);

        try {
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fullPath;

            webClient.put()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // public URL - safe encoding of path components (filename already sanitized)
            String encoded = URLEncoder.encode(fullPath, StandardCharsets.UTF_8);
            // Supabase public url pattern uses raw path; DO NOT double-encode slashes. So create manually:
            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fullPath;
            return publicUrl;
        } catch (Exception ex) {
            throw new FileStorageException("Failed to upload file to Supabase", ex);
        }
    }
}
