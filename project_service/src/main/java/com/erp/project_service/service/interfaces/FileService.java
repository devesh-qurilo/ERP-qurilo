package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.file.FileMetaDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    FileMetaDto uploadProjectFile(Long projectId, MultipartFile file, String uploadedBy);
    FileMetaDto uploadTaskFile(Long taskId, MultipartFile file, String uploadedBy);
    List<FileMetaDto> listProjectFiles(Long projectId);
    List<FileMetaDto> listTaskFiles(Long taskId);
    void deleteFile(Long fileId, String actor);
    byte[] downloadFile(Long fileId); // or return InputStreamResource in controller
}
