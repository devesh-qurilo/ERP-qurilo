package com.erp.employee_service.dto.holiday;

import jakarta.validation.Valid;
import lombok.Data;


import java.util.List;

@Data
public class BulkHolidayRequestDto {

    @Valid
    private List<HolidayRequestDto> holidays;
}