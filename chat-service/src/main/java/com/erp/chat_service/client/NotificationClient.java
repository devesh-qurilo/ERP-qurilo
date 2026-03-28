package com.erp.chat_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "employee-service",
        contextId = "notificationClient",
        path = "/employee/notifications",
        configuration = FeignInternalAuthConfig.class
)
public interface NotificationClient {

    @PostMapping("/internal/send")
    void sendNotification(@RequestBody SendNotificationRequest request);

    @PostMapping("/internal/send-many")
    void sendNotificationToMany(@RequestBody SendNotificationManyRequest request);
}
