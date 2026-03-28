package com.erp.employee_service.dto.holiday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidayRequestDto {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotBlank(message = "Occasion is required")
    private String occasion;
}