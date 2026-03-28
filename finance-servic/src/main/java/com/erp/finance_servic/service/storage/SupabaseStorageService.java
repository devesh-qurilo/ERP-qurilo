package com.erp.finance_servic.service.storage;

import com.erp.finance_servic.dto.storage.FileMetaDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

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

    public SupabaseStorageService(@Qualifier("supabaseWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @PostConstruct
    public void init() {
        if (supabaseUrl != null && supabaseUrl.endsWith("/")) {
            supabaseUrl = supabaseUrl.substring(0, supabaseUrl.length() - 1);
        }
        log.info("SupabaseStorageService initialized with url='{}' bucket='{}'", supabaseUrl, bucket);
        if (supabaseKey == null || supabaseKey.isBlank()) {
            log.warn("supabase.service-role-key is not configured! Upload/delete will fail without service role key.");
        }
    }

    /**
     * Upload file to Supabase storage. Returns FileMetaDto with public URL.
     * folder example: "invoices/INV-2025-001"  (you may pass only folder or folder/filename)
     */
    public FileMetaDto uploadFile(MultipartFile file, String folder, String uploadedBy) {
        try {
            if (file == null || file.isEmpty()) throw new IllegalArgumentException("file is empty");

            String orig = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            orig = sanitizeFilename(orig);

            String folderTrim = folder == null ? "" : folder.replaceAll("^/+", "").replaceAll("/+$", "");
            String objectPath;
            if (!folderTrim.isEmpty() && folderTrim.endsWith(orig)) {
                objectPath = folderTrim;
            } else {
                String safeName = UUID.randomUUID() + "-" + orig;
                objectPath = folderTrim.isEmpty() ? safeName : folderTrim + "/" + safeName;
            }

            String encodedPath = encodePathPreserveSlashes(objectPath);

            // PUT bytes to Supabase storage
            webClient.put()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, encodedPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .bodyValue(file.getBytes())
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(),
                            resp -> resp.bodyToMono(String.class).flatMap(body -> {
                                log.error("Supabase upload failed: status={}, body={}", resp.statusCode(), body);
                                return Mono.error(new RuntimeException("Supabase upload failed: " + resp.statusCode()));
                            }))
                    .bodyToMono(Void.class)
                    .block();

            // public url (public buckets) - clients can fetch this directly
            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + encodedPath;

            FileMetaDto meta = new FileMetaDto();
            meta.setBucket(bucket);
            meta.setPath(objectPath);   // internal path (not encoded) - store this in DB
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

    public void deleteFile(String path) {
        try {
            if (path == null || path.isBlank()) return;
            String encodedPath = encodePathPreserveSlashes(path);

            webClient.delete()
                    .uri("/storage/v1/object/{bucket}/{path}", bucket, encodedPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .retrieve()
                    .onStatus(s -> !s.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).flatMap(body -> {
                        log.warn("Supabase delete returned {} -> {}", resp.statusCode(), body);
                        return Mono.error(new RuntimeException("Supabase delete failed: " + resp.statusCode()));
                    }))
                    .bodyToMono(Void.class)
                    .block();

            log.info("Deleted storage object: {}", path);
        } catch (Exception e) {
            log.warn("Failed to delete storage object {}: {}", path, e.getMessage());
        }
    }

    /**
     * List objects under prefix (returns list of object names)
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

            if (res == null) return Collections.emptyList();

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

    // ----------------- helpers -----------------

    private String encodePathPreserveSlashes(String path) {
        if (path == null) return "";
        String[] segments = path.split("/");
        return Arrays.stream(segments)
                .map(seg -> {
                    try {
                        return URLEncoder.encode(seg, StandardCharsets.UTF_8.name()).replace("+", "%20");
                    } catch (Exception e) {
                        return seg.replaceAll("[^A-Za-z0-9.\\-_%()\\[\\]]+", "");
                    }
                })
                .collect(Collectors.joining("/"));
    }

    private String sanitizeFilename(String input) {
        if (input == null) return UUID.randomUUID().toString();
        String name = input.replaceAll("[\\\\/]+", "");
        name = name.trim().replaceAll("\\s+", "-");
        name = name.replaceAll("[^A-Za-z0-9.\\-_%()\\[\\]]+", "");
        if (name.isEmpty()) return UUID.randomUUID().toString();
        return name;
    }
}
