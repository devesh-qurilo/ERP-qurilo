package com.erp.project_service.service.notification;

import com.erp.project_service.client.NotificationClient;
import com.erp.project_service.client.SendBulkNotificationRequest;
import com.erp.project_service.client.SendNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationHelperService {

    private final NotificationClient notificationClient;

    @Async
    public void sendNotification(String senderEmployeeId, String receiverEmployeeId,
                                 String title, String message, String type) {
        try {
            SendNotificationRequest request = new SendNotificationRequest();
            // Note: senderEmployeeId is removed from DTO, will be handled by authentication
            request.setReceiverEmployeeId(receiverEmployeeId);
            request.setTitle(title);
            request.setMessage(message);
            request.setType(type);

            notificationClient.sendNotification(request);
            log.info("Notification sent to {}: {}", receiverEmployeeId, title);
        } catch (Exception e) {
            log.error("Failed to send notification to {}: {}", receiverEmployeeId, e.getMessage());
            // Don't throw exception - notification failure shouldn't break main operation
        }
    }

    @Async
    public void sendBulkNotification(String senderEmployeeId, List<String> receiverEmployeeIds,
                                     String title, String message, String type) {
        if (receiverEmployeeIds == null || receiverEmployeeIds.isEmpty()) {
            log.warn("No receiver employee IDs provided for bulk notification");
            return;
        }

        try {
            SendBulkNotificationRequest request = new SendBulkNotificationRequest();
            // Note: senderEmployeeId is removed from DTO, will be handled by authentication
            request.setReceiverEmployeeIds(receiverEmployeeIds);
            request.setTitle(title);
            request.setMessage(message);
            request.setType(type);

            notificationClient.sendBulkNotification(request);
            log.info("Bulk notification sent to {} employees: {}", receiverEmployeeIds.size(), title);
        } catch (Exception e) {
            log.error("Failed to send bulk notification to {} employees: {}",
                    receiverEmployeeIds.size(), e.getMessage());
            // Don't throw exception - notification failure shouldn't break main operation
        }
    }

    // Common notification templates
    public void notifyTaskCreated(String actor, List<String> assignedEmployees, String taskTitle, Long taskId) {
        String title = "🎯 New Task Assigned";
        String message = String.format(
                "You have been assigned a new task: '%s' (ID: %d). " +
                        "Please review the task details and start working on it.",
                taskTitle, taskId
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "TASK_ASSIGNED");
    }

    public void notifyTaskUpdated(String actor, List<String> assignedEmployees, String taskTitle, Long taskId) {
        String title = "📝 Task Updated";
        String message = String.format(
                "Task '%s' has been updated (ID: %d). " +
                        "Please check the updated details.",
                taskTitle, taskId
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "TASK_UPDATED");
    }

    public void notifyTaskStatusChanged(String actor, List<String> assignedEmployees, String taskTitle, String newStatus) {
        String title = "🔄 Task Status Changed";
        String message = String.format(
                "Task '%s' status has been changed to: %s. " +
                        "Current status reflects the latest progress.",
                taskTitle, newStatus
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "TASK_STATUS_CHANGE");
    }

    public void notifyTaskReminder(String actor, List<String> assignedEmployees, String taskTitle, Long taskId) {
        String title = "🔔 Task Reminder";
        String message = String.format(
                "Reminder: Please check the progress of task '%s' (ID: %d). " +
                        "Ensure timely completion and update the status if needed.",
                taskTitle, taskId
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "TASK_REMINDER");
    }

    public void notifyTaskApproved(String actor, List<String> assignedEmployees, String taskTitle, Long taskId) {
        String title = "✅ Task Approved";
        String message = String.format(
                "Your task '%s' has been approved by admin (ID: %d). " +
                        "You can now start working on this task.",
                taskTitle, taskId
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "TASK_APPROVAL");
    }

    public void notifyProjectAssigned(String actor, List<String> assignedEmployees, String projectName, Long projectId) {
        String title = "🏢 Project Assignment";
        String message = String.format(
                "You have been assigned to project: '%s' (ID: %d). " +
                        "Please check the project details and tasks.",
                projectName, projectId
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "PROJECT_ASSIGNED");
    }

    public void notifyTimeLogCreated(String actor, String employeeId, String projectName) {
        String title = "⏱️ Time Log Created";
        String message = String.format("Time log created for project: %s", projectName);
        sendNotification(actor, employeeId, title, message, "TIMELOG_CREATED");
    }

    public void notifyRecurringTaskCreated(String actor, List<String> assignedEmployees, String taskTitle) {
        String title = "🔄 Recurring Task Assigned";
        String message = String.format(
                "You have been assigned to recurring task: '%s'. " +
                        "This task will repeat according to the schedule.",
                taskTitle
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "RECURRING_TASK_ASSIGNED");
    }

    public void notifyWeeklyTimesheet(String actor, String employeeId, String weekRange, Integer totalHours, String action) {
        String title = "";
        String message = "";

        if ("CREATED".equals(action)) {
            title = "📅 Weekly Timesheet Submitted";
            message = String.format(
                    "Your weekly timesheet for %s has been successfully submitted. " +
                            "Total hours logged: %d hours. The timesheet is now pending review.",
                    weekRange, totalHours
            );
        } else if ("UPDATED".equals(action)) {
            title = "📝 Weekly Timesheet Updated";
            message = String.format(
                    "Your weekly timesheet for %s has been updated. " +
                            "Total hours logged: %d hours. Changes have been saved successfully.",
                    weekRange, totalHours
            );
        } else if ("APPROVED".equals(action)) {
            title = "✅ Weekly Timesheet Approved";
            message = String.format(
                    "Your weekly timesheet for %s has been approved. " +
                            "Total hours logged: %d hours. The timesheet is now finalized.",
                    weekRange, totalHours
            );
        } else if ("REJECTED".equals(action)) {
            title = "❌ Weekly Timesheet Rejected";
            message = String.format(
                    "Your weekly timesheet for %s has been rejected. " +
                            "Please review and resubmit with corrections.",
                    weekRange
            );
        }

        if (!title.isEmpty() && !message.isEmpty()) {
            sendNotification(actor, employeeId, title, message, "WEEKLY_TIMESHEET");
        }
    }

    // New method for milestone notifications
    public void notifyMilestoneCompleted(String actor, List<String> assignedEmployees, String milestoneTitle, Long projectId) {
        String title = "🎉 Milestone Completed";
        String message = String.format(
                "Milestone '%s' has been completed for project (ID: %d). " +
                        "Great work team!",
                milestoneTitle, projectId
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "MILESTONE_COMPLETED");
    }

    // New method for deadline reminders
    public void notifyDeadlineApproaching(String actor, List<String> assignedEmployees, String taskTitle, String deadline) {
        String title = "⏰ Deadline Approaching";
        String message = String.format(
                "Task '%s' deadline is approaching: %s. " +
                        "Please ensure timely completion.",
                taskTitle, deadline
        );
        sendBulkNotification(actor, assignedEmployees, title, message, "DEADLINE_REMINDER");
    }
}