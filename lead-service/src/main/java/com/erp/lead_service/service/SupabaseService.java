package com.erp.lead_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface SupabaseService {
    String uploadFile(MultipartFile file, Long dealId);
    void deleteFileByUrl(String url);
    public void deleteFileByObjectPath(String objectPath);
}
