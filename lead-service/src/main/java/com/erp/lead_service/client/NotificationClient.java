package com.erp.lead_service.client;

import com.erp.lead_service.dto.dto.notification.SendNotificationDto;
import com.erp.lead_service.dto.dto.notification.SendNotificationManyDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "employee-service", url = "${employee-service.url}")
public interface NotificationClient {

    @PostMapping("/employee/notifications/send")
    void send(@RequestBody SendNotificationDto dto,
              @RequestHeader("Authorization") String authorization);

    @PostMapping("/employee/notifications/send-many")
    void sendMany(@RequestBody SendNotificationManyDto dto,
                  @RequestHeader("Authorization") String authorization);
}
