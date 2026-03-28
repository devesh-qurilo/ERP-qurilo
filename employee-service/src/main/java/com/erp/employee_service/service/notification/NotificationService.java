package com.erp.employee_service.service.notification;

import com.erp.employee_service.dto.notification.NotificationDto;
import com.erp.employee_service.dto.notification.SendNotificationDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {
    NotificationDto getById(Long id);
    List<NotificationDto> getMyNotifications(String employeeId);
    void markRead(Long id, String employeeId);
    void markUnread(Long id, String employeeId);
    void clearAll(String employeeId);
    CompletableFuture<Void> sendNotification(String senderEmployeeId, SendNotificationDto dto);
    CompletableFuture<Void> sendNotificationMany(String senderEmployeeId, List<String> receiverEmployeeIds, String title, String message, String type);
    void deleteById(Long id, String employeeId);
}