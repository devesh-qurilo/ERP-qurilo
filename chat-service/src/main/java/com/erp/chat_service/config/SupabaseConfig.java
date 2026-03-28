package com.erp.chat_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

@Slf4j
@Configuration
public class SupabaseConfig {

    @Value("${project.supabase.url}")
    private String supabaseUrl;

    @Value("${project.supabase.bucket:ERP-BUCKET}")
    private String bucket;

    @Value("${project.supabase.s3-access-key}")
    private String s3AccessKey;

    @Value("${project.supabase.s3-secret-key}")
    private String s3SecretKey;

    @Value("${chat.attachments.enabled:true}")
    private boolean fileUploadEnabled;

    @Bean
    public S3Client s3Client() {
        log.info("=== Supabase S3 Configuration ===");
        log.info("URL: {}", supabaseUrl);
        log.info("Bucket: {}", bucket);
        log.info("File Upload Enabled: {}", fileUploadEnabled);

        if (!fileUploadEnabled) {
            log.warn("File upload is disabled. Using mock S3 client.");
            return createMockS3Client();
        }

        if (isBlank(supabaseUrl) || isBlank(bucket) || isBlank(s3AccessKey) || isBlank(s3SecretKey)) {
            throw new IllegalStateException("Supabase S3 configuration is incomplete");
        }

        try {
            // Correct S3-compatible endpoint: DO NOT append the bucket here
            String s3Endpoint = supabaseUrl + "/storage/v1/s3";
            log.info("S3 Endpoint: {}", s3Endpoint);

            S3Client client = S3Client.builder()
                    .endpointOverride(URI.create(s3Endpoint))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(s3AccessKey, s3SecretKey)
                            )
                    )
                    // Supabase S3 uses SigV4 with a neutral region; us-east-1 is fine
                    .region(Region.of("us-east-1"))
                    .forcePathStyle(true)
                    .build();

            testSupabaseConnection(client);
            log.info("✅ Supabase S3 client initialized successfully");
            return client;

        } catch (Exception e) {
            log.error("❌ Failed to initialize Supabase S3 client: {}", e.getMessage());
            throw new RuntimeException("Supabase configuration failed: " + e.getMessage(), e);
        }
    }

    private void testSupabaseConnection(S3Client client) {
        try {
            log.info("Testing Supabase connection (HeadBucket {}) ...", bucket);
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("✅ Successfully connected to Supabase bucket: {}", bucket);
        } catch (S3Exception e) {
            if (e.statusCode() == 403) {
                log.error("❌ Access denied to bucket {}. Check S3 credentials and bucket policies.", bucket);
            } else if (e.statusCode() == 404) {
                log.error("❌ Bucket not found: {}. Create it in Supabase Storage.", bucket);
            } else {
                log.error("❌ Supabase connection test failed: {}", e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
            }
            throw e;
        }
    }

    private S3Client createMockS3Client() {
        log.warn("Using mock S3 client - files will NOT be uploaded to Supabase (mock)");
        // A minimal builder-based client pointed at localhost so calls fail gracefully
        try {
            return S3Client.builder()
                    .region(Region.of("us-east-1"))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("mock","mock")))
                    .endpointOverride(URI.create("http://127.0.0.1:4566")) // localstack or unreachable endpoint OK
                    .forcePathStyle(true)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to create mock S3 client builder: {}", e.getMessage());
            // As a last resort, throw a runtime exception so startup fails loudly
            throw new IllegalStateException("Cannot initialize mock S3 client", e);
        }
    }


    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
