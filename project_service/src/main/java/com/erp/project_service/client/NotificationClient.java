package com.erp.project_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "employee-service",
        contextId = "notificationClient",  // Unique context ID
        path = "/employee/notifications"
)
public interface NotificationClient {

    @PostMapping("/internal/send")
    void sendNotification(@RequestBody SendNotificationRequest request);

    @PostMapping("/internal/send-many")
    void sendBulkNotification(@RequestBody SendBulkNotificationRequest request);
}