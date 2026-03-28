//package com.erp.project_service.dto.timesheet;
//
//import jakarta.validation.constraints.NotNull;
//import lombok.Data;
//
//import java.time.LocalDate;
//
//@Data
//public class WeeklyTimeLogCreateRequest {
//    private String employeeId;
//    private Long taskId;
//    @NotNull
//    private LocalDate weekStartDate; // e.g., Monday
//    // hours per day 0-24
//    private Integer day1Hours;
//    private Integer day2Hours;
//    private Integer day3Hours;
//    private Integer day4Hours;
//    private Integer day5Hours;
//    private Integer day6Hours;
//    private Integer day7Hours;
//}

package com.erp.project_service.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyTimeLogCreateRequest {

    /**
     * Project ID (optional but recommended)
     */
    private Long projectId;

    /**
     * Task ID – UI me jo task select karega
     */
    private Long taskId;

    /**
     * Admin agar kisi aur ke liye fill kare to
     * employeeId pass karega, warna null -> current user
     */
    private String employeeId;

    /**
     * 7 dates ka list – har item me date + hours
     */
    private List<WeeklyTimeLogDayRequest> days;
}

