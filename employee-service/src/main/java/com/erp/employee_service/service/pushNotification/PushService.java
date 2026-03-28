package com.erp.employee_service.service.pushNotification;

import java.util.Map;
import java.util.List;

public interface PushService {
    void registerToken(String employeeId, String provider, String token, String deviceInfo);
    void unregisterToken(String token);
    void sendPushToUser(String employeeId, String title, String body, Map<String, String> data);
    void sendPushToUsers(List<String> employeeIds, String title, String body, Map<String, String> data);
}
