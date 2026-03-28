package com.erp.chat_service.service;

import com.erp.chat_service.dto.ChatMessageResponse;
import com.erp.chat_service.entity.ChatMessage;
import com.erp.chat_service.entity.MessageStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmployeeService employeeService;

    public void sendMessageToUser(ChatMessage message, String receiverId) {
        try {
            ChatMessageResponse response = convertToResponse(message, receiverId);

            // Send to specific user
            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/messages",
                    response
            );

            log.debug("Message sent via WebSocket to user: {}", receiverId);

        } catch (Exception e) {
            log.error("Error sending WebSocket message to user: {}", receiverId, e);
        }
    }

    public void sendTypingIndicator(String senderId, String receiverId, boolean isTyping) {
        try {
            TypingIndicator typingIndicator = new TypingIndicator(senderId, isTyping);
            messagingTemplate.convertAndSendToUser(
                    receiverId,
                    "/queue/typing",
                    typingIndicator
            );
        } catch (Exception e) {
            log.error("Error sending typing indicator", e);
        }
    }

    public void sendMessageStatusUpdate(Long messageId, String userId, MessageStatus status) {
        try {
            MessageStatusUpdate statusUpdate = new MessageStatusUpdate(messageId, status);
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/message-status",
                    statusUpdate
            );
        } catch (Exception e) {
            log.error("Error sending message status update", e);
        }
    }

    public void sendOnlineStatus(String employeeId, boolean isOnline) {
        try {
            OnlineStatusUpdate statusUpdate = new OnlineStatusUpdate(employeeId, isOnline);
            messagingTemplate.convertAndSend(
                    "/topic/online-status",
                    statusUpdate
            );
        } catch (Exception e) {
            log.error("Error sending online status update", e);
        }
    }

    private ChatMessageResponse convertToResponse(ChatMessage message, String currentUserId) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setChatRoomId(message.getChatRoomId());
        response.setSenderId(message.getSenderId());
        response.setReceiverId(message.getReceiverId());
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setStatus(message.getStatus());
        response.setCreatedAt(message.getCreatedAt());
        response.setDeletedForCurrentUser(message.getDeletedForUsers().contains(currentUserId));

        return response;
    }

    // Inner classes for WebSocket messages
    public static class TypingIndicator {
        private String senderId;
        private boolean typing;

        public TypingIndicator() {}

        public TypingIndicator(String senderId, boolean typing) {
            this.senderId = senderId;
            this.typing = typing;
        }

        // Getters and setters
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public boolean isTyping() { return typing; }
        public void setTyping(boolean typing) { this.typing = typing; }
    }

    public static class MessageStatusUpdate {
        private Long messageId;
        private MessageStatus status;

        public MessageStatusUpdate() {}

        public MessageStatusUpdate(Long messageId, MessageStatus status) {
            this.messageId = messageId;
            this.status = status;
        }

        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
        public MessageStatus getStatus() { return status; }
        public void setStatus(MessageStatus status) { this.status = status; }
    }

    public static class OnlineStatusUpdate {
        private String employeeId;
        private boolean online;

        public OnlineStatusUpdate() {}

        public OnlineStatusUpdate(String employeeId, boolean online) {
            this.employeeId = employeeId;
            this.online = online;
        }

        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        public boolean isOnline() { return online; }
        public void setOnline(boolean online) { this.online = online; }
    }
}