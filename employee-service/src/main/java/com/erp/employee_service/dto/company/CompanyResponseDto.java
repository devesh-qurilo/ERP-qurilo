package com.erp.employee_service.dto.company;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CompanyResponseDto {
    private Long id;
    private String companyName;
    private String email;
    private String contactNo;
    private String website;
    private String address;
    private String logoUrl;
    private Long logoId;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}