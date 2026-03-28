package com.erp.lead_service.dto.deal;

import lombok.Data;
import java.util.List;

@Data
public class DealEmployeeRequestDto {
    private List<String> employeeIds;
}
