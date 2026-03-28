package com.erp.chat_service.service;

import com.erp.chat_service.client.NotificationClient;
import com.erp.chat_service.client.SendNotificationManyRequest;
import com.erp.chat_service.client.SendNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationClient notificationClient;

    /**
     * Send notification when a new message is received
     */
//    @Async
    public void sendMessageNotification(String senderId, String receiverId, String messageContent, boolean hasFile) {
        try {
            String title = "New Message";
            String message = buildNotificationMessage(senderId, messageContent, hasFile);

            SendNotificationRequest request = new SendNotificationRequest();
            request.setReceiverEmployeeId(receiverId);
            request.setTitle(title);
            request.setMessage(message);
            request.setType("CHAT_MESSAGE");

            notificationClient.sendNotification(request);
            log.info("Chat notification sent to: {}", receiverId);

        } catch (Exception e) {
            log.error("Failed to send chat notification to {}: {}", receiverId, e.getMessage());
            // Don't throw exception - notification failure shouldn't break chat
        }
    }

    /**
     * Send notification when multiple users are added to a group (future feature)
     */
//    @Async
    public void sendGroupMessageNotification(String senderId, List<String> receiverIds, String groupName, String messageContent) {
        try {
            String title = "New Group Message";
            String message = String.format("%s sent a message in %s: %s",
                    getEmployeeDisplay(senderId), groupName,
                    truncateMessage(messageContent));

            SendNotificationManyRequest request = new SendNotificationManyRequest();
            request.setReceiverEmployeeIds(receiverIds);
            request.setTitle(title);
            request.setMessage(message);
            request.setType("GROUP_CHAT");

            notificationClient.sendNotificationToMany(request);
            log.info("Group chat notification sent to {} users", receiverIds.size());

        } catch (Exception e) {
            log.error("Failed to send group chat notifications: {}", e.getMessage());
        }
    }

    /**
     * Send notification when message is read (read receipt)
     */
//    @Async
    public void sendReadReceiptNotification(String readerId, String senderId, Long messageId) {
        try {
            String title = "Message Read";
            String message = String.format("%s read your message", getEmployeeDisplay(readerId));

            SendNotificationRequest request = new SendNotificationRequest();
            request.setReceiverEmployeeId(senderId);
            request.setTitle(title);
            request.setMessage(message);
            request.setType("CHAT_READ_RECEIPT");

            notificationClient.sendNotification(request);
            log.info("Read receipt notification sent to: {}", senderId);

        } catch (Exception e) {
            log.error("Failed to send read receipt notification: {}", e.getMessage());
        }
    }

    /**
     * Send notification for file sharing
     */
//    @Async
    public void sendFileSharedNotification(String senderId, String receiverId, String fileName) {
        try {
            String title = "File Shared";
            String message = String.format("%s shared a file: %s",
                    getEmployeeDisplay(senderId), fileName);

            SendNotificationRequest request = new SendNotificationRequest();
            request.setReceiverEmployeeId(receiverId);
            request.setTitle(title);
            request.setMessage(message);
            request.setType("FILE_SHARED");

            notificationClient.sendNotification(request);
            log.info("File shared notification sent to: {}", receiverId);

        } catch (Exception e) {
            log.error("Failed to send file shared notification: {}", e.getMessage());
        }
    }

    private String buildNotificationMessage(String senderId, String messageContent, boolean hasFile) {
        String senderDisplay = getEmployeeDisplay(senderId);

        if (hasFile) {
            return String.format("%s sent you a file", senderDisplay);
        } else if (messageContent != null && !messageContent.trim().isEmpty()) {
            return String.format("%s: %s", senderDisplay, truncateMessage(messageContent));
        } else {
            return String.format("%s sent you a message", senderDisplay);
        }
    }

    private String getEmployeeDisplay(String employeeId) {
        // For now, just return the employee ID
        // You can enhance this to fetch employee name from employee service
        return employeeId;
    }

    private String truncateMessage(String message) {
        if (message == null) return "";
        if (message.length() <= 50) return message;
        return message.substring(0, 47) + "...";
    }
}