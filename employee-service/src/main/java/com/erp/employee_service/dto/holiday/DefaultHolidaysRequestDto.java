package com.erp.employee_service.dto.holiday;

import lombok.Data;

import java.util.List;

@Data
public class DefaultHolidaysRequestDto {

    private List<String> weekDays; // MONDAY, TUESDAY, etc.
    private String occasion;
    private Integer year;
    private Integer month;
}