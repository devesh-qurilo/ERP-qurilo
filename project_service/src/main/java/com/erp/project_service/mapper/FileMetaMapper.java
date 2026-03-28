package com.erp.project_service.mapper;

import com.erp.project_service.dto.file.FileMetaDto;
import com.erp.project_service.entity.FileMeta;

public final class FileMetaMapper {
    private FileMetaMapper() {}

        public static FileMetaDto toDto(FileMeta e) {
            var d = new FileMetaDto();
            d.setId(e.getId());
            d.setProjectId(e.getProjectId());
            d.setTaskId(e.getTaskId());
            d.setFilename(e.getFilename());
            d.setBucket(e.getBucket());
            d.setPath(e.getPath());
            d.setUrl(e.getUrl());
            d.setMimeType(e.getMimeType());
            d.setSize(e.getSize());
            d.setUploadedBy(e.getUploadedBy());
            d.setCreatedAt(e.getCreatedAt());
            return d;
        }


    public static FileMeta toEntity(FileMetaDto dto) {
        if (dto == null) return null;
        return FileMeta.builder()
                .id(dto.getId())
                .projectId(dto.getProjectId())
                .taskId(dto.getTaskId())
                .bucket(dto.getBucket())
                .path(dto.getPath())
                .filename(dto.getFilename())
                .url(dto.getUrl())
                .mimeType(dto.getMimeType())
                .size(dto.getSize())
                .uploadedBy(dto.getUploadedBy())
                .build();
    }
}
