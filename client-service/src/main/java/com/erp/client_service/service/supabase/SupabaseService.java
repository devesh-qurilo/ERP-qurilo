package com.erp.client_service.service.supabase;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface SupabaseService {
    String uploadFile(MultipartFile file, String path, boolean isProfilePicture) throws IOException;
}
