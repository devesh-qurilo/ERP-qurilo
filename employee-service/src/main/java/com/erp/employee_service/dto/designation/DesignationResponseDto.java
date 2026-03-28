package com.erp.employee_service.dto.designation;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DesignationResponseDto {
    private Long id;
    private String designationName;
    private Long parentDesignationId;
    private String parentDesignationName;
    private LocalDate createDate;
}
