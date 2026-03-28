package com.erp.employee_service.event.listner;

import com.erp.employee_service.event.EmployeeCreatedEvent;
import com.erp.employee_service.service.leave.LeaveQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmployeeCreatedListener {

    private final LeaveQuotaService leaveQuotaService;

    @EventListener
    public void onEmployeeCreated(EmployeeCreatedEvent evt) {
        // assign defaults
        leaveQuotaService.assignDefaultsIfMissing(evt.getEmployee());
    }
}
