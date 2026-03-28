package com.erp.lead_service.service;

import com.erp.lead_service.entity.DealFollowUp;

public interface ReminderSchedulerService {
    void scheduleFollowupReminder(DealFollowUp followup, String authHeader);

    void cancelScheduledFollowup(DealFollowUp saved);
}
