package com.erp.employee_service.dto.holiday;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HolidayResponseDto {

    private Long id;
    private LocalDate date;
    private String day;
    private String occasion;
    private Boolean isDefaultWeekly;
    private Boolean isActive;
}