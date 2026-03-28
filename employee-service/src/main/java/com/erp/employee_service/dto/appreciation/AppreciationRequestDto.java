package com.erp.employee_service.dto.appreciation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class AppreciationRequestDto {

    @NotNull(message = "Award ID is required")
    private Long awardId;

    @NotNull(message = "Employee ID is required")
    private String givenToEmployeeId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String summary;

    private MultipartFile photoFile;
}