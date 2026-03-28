package com.erp.project_service.service.interfaces;

public interface TaskReminderService {
    void sendReminder(Long taskId, String sentBy);
}