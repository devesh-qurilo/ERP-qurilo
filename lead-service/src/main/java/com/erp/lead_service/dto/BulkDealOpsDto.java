package com.erp.lead_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkDealOpsDto {
    private List<String> tags;
    private List<String> employeeIds;
    private List<CommentRequestDto> comments;
}
