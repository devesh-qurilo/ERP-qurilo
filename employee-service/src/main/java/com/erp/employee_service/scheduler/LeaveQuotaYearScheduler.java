package com.erp.employee_service.scheduler;

import com.erp.employee_service.service.leave.LeaveQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class LeaveQuotaYearScheduler {

    private final LeaveQuotaService leaveQuotaService;

    // Runs every year on 1st January at 00:05 (Lithuania time)
    @Scheduled(cron = "0 5 0 1 1 ?", zone = "Europe/Vilnius")
    public void refreshLeaveQuota() {
        leaveQuotaService.refreshLeaveQuotaForNewYear();
    }
}
