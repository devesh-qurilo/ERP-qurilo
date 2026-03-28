package com.erp.project_service.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyTimeLogDayRequest {

    /**
     * Jis date ke liye timelog bharna hai
     */
    private LocalDate date;

    /**
     * Working hours for this date, e.g. 4, 7.5
     */
    private BigDecimal hours;

    /**
     * Optional memo / notes per-day
     */
    private String memo;
}
