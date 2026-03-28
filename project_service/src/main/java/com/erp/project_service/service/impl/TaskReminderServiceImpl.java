package com.erp.project_service.service.impl;

import com.erp.project_service.entity.Task;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.repository.TaskRepository;
import com.erp.project_service.service.interfaces.TaskReminderService;
import com.erp.project_service.service.notification.NotificationHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskReminderServiceImpl implements TaskReminderService {

    private final TaskRepository taskRepository;
    private final NotificationHelperService notificationHelperService;

    @Override
    @Transactional
    public void sendReminder(Long taskId, String sentBy) {
        try {
            // Fetch the task with assigned employees
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new NotFoundException("Task not found with id: " + taskId));

            // Check if task has assigned employees
            if (task.getAssignedEmployeeIds() == null || task.getAssignedEmployeeIds().isEmpty()) {
                log.warn("No assigned employees found for task: {}", taskId);
                throw new NotFoundException("No assigned employees found for this task");
            }

            // Prepare predefined reminder message
            String title = "🔔 Task Reminder";
            String message = String.format(
                    "Reminder: Please check the progress of task '%s' (ID: %d). " +
                            "Ensure timely completion and update the status if needed.",
                    task.getTitle(),
                    taskId
            );

            // Send notifications to all assigned employees
            notificationHelperService.sendBulkNotification(
                    sentBy,
                    new ArrayList<>(task.getAssignedEmployeeIds()),
                    title,
                    message,
                    "TASK_REMINDER"
            );

            log.info("Reminder sent for task {} to {} employees by {}",
                    taskId, task.getAssignedEmployeeIds().size(), sentBy);

        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send reminder for task {}: {}", taskId, e.getMessage());
            throw new RuntimeException("Failed to send reminder: " + e.getMessage());
        }
    }
}