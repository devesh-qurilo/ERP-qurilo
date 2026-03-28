package com.erp.employee_service.dto.appreciation;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppreciationResponseDto {

    private Long id;
    private Long awardId;
    private String awardTitle;
    private String givenToEmployeeId;
    private String givenToEmployeeName;
    private LocalDate date;
    private String summary;
    private String photoUrl;
    private Long photoFileId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}