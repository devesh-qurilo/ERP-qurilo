//    package com.erp.chat_service.service;
//
//    import com.erp.chat_service.dto.ChatMessageResponse;
//    import com.erp.chat_service.dto.ChatRoomResponse;
//    import com.erp.chat_service.dto.FileAttachmentDTO;
//    import com.erp.chat_service.dto.SendMessageRequest;
//    import com.erp.chat_service.entity.*;
//    import com.erp.chat_service.repository.ChatMessageRepository;
//    import com.erp.chat_service.repository.ChatRoomRepository;
//    import lombok.extern.slf4j.Slf4j;
//    import org.springframework.beans.factory.annotation.Autowired;
//    import org.springframework.stereotype.Service;
//    import org.springframework.transaction.annotation.Transactional;
//    import org.springframework.web.multipart.MultipartFile;
//
//    import java.util.List;
//    import java.util.Optional;
//    import java.util.stream.Collectors;
//
//    @Slf4j
//    @Service
//    @Transactional
//    public class ChatService {
//
//        @Autowired
//        private ChatMessageRepository messageRepository;
//
//        @Autowired
//        private ChatRoomRepository chatRoomRepository;
//
//        @Autowired
//        private FileStorageService fileStorageService;
//
//        @Autowired
//        private WebSocketService webSocketService;
//
//        @Autowired
//        private EmployeeService employeeService;
//
//        @Autowired
//        private NotificationService notificationService;
//
//        public ChatMessage sendMessage(SendMessageRequest request, String senderId) {
//            try {
//                log.info("=== SEND MESSAGE DEBUG ===");
//                log.info("Sender: {}, Receiver: {}", senderId, request.getReceiverId());
//                log.info("File present: {}", request.getFile() != null);
//                log.info("File name: {}", request.getFile() != null ? request.getFile().getOriginalFilename() : "null");
//                log.info("Message type: {}", request.getMessageType());
//
//                // Get or create chat room
//                String chatRoomId = generateChatRoomId(senderId, request.getReceiverId());
//                ChatRoom chatRoom = getOrCreateChatRoom(senderId, request.getReceiverId(), chatRoomId);
//
//                // Handle file upload if present
//                FileAttachment fileAttachment = null;
//                MessageType messageType = request.getMessageType();
//
//                if (request.getFile() != null && !request.getFile().isEmpty()) {
//                    log.info("Attempting file upload...");
//                    fileAttachment = fileStorageService.uploadFile(request.getFile());
//                    messageType = getMessageTypeFromFile(request.getFile().getContentType());
//                    log.info("File upload result - Attachment: {}", fileAttachment != null ? fileAttachment.getId() : "NULL");
//                } else {
//                    log.info("No file provided or file is empty");
//                }
//
//                // Create and save message
//                ChatMessage message = new ChatMessage();
//                message.setChatRoomId(chatRoomId);
//                message.setSenderId(senderId);
//                message.setReceiverId(request.getReceiverId());
//                message.setContent(request.getContent());
//                message.setMessageType(messageType);
//                message.setFileAttachment(fileAttachment); // This might be null
//                message.setStatus(MessageStatus.SENT);
//
//                log.info("Saving message with file attachment: {}", fileAttachment != null);
//                ChatMessage savedMessage = messageRepository.save(message);
//
//                // Check if file attachment is persisted
//                if (savedMessage.getFileAttachment() != null) {
//                    log.info("Message saved with file attachment ID: {}", savedMessage.getFileAttachment().getId());
//                } else {
//                    log.warn("Message saved but file attachment is NULL");
//                }
//
//                // Send real-time notification via WebSocket
//                webSocketService.sendMessageToUser(savedMessage, request.getReceiverId());
//
//                // Send push notification to receiver
//                sendMessageNotification(savedMessage);
//
//                return savedMessage;
//
//            } catch (Exception e) {
//                log.error("Error sending message from {} to {}", senderId, request.getReceiverId(), e);
//                throw new RuntimeException("Failed to send message: " + e.getMessage());
//            }
//        }
//
//        /**
//         * Send notification for new message
//         */
//        private void sendMessageNotification(ChatMessage message) {
//            try {
//                boolean hasFile = message.getFileAttachment() != null;
//                String messageContent = message.getContent();
//
//                notificationService.sendMessageNotification(
//                        message.getSenderId(),
//                        message.getReceiverId(),
//                        messageContent,
//                        hasFile
//                );
//
//                // If it's a file message, send additional file shared notification
//                if (hasFile) {
//                    notificationService.sendFileSharedNotification(
//                            message.getSenderId(),
//                            message.getReceiverId(),
//                            message.getFileAttachment().getFileName()
//                    );
//                }
//
//            } catch (Exception e) {
//                log.error("Failed to send message notification: {}", e.getMessage());
//                // Don't throw - notification failure shouldn't break chat
//            }
//        }
//
//        public void deleteMessageForUser(Long messageId, String employeeId) {
//            try {
//                ChatMessage message = messageRepository.findById(messageId)
//                        .orElseThrow(() -> new IllegalArgumentException("Message not found"));
//
//                // Check if user is either sender or receiver
//                if (!message.getSenderId().equals(employeeId) && !message.getReceiverId().equals(employeeId)) {
//                    throw new IllegalArgumentException("You can only delete your own messages");
//                }
//
//                message.getDeletedForUsers().add(employeeId);
//                messageRepository.save(message);
//                log.info("Message {} deleted for user {}", messageId, employeeId);
//
//            } catch (Exception e) {
//                log.error("Error deleting message {} for user {}", messageId, employeeId, e);
//                throw new RuntimeException("Failed to delete message: " + e.getMessage());
//            }
//        }
//
//        public List<ChatMessageResponse> getChatHistory(String currentUserId, String otherUserId) {
//            try {
//                String chatRoomId = generateChatRoomId(currentUserId, otherUserId);
//
//                // Use the new method that eagerly fetches file attachments
//                List<ChatMessage> messages = messageRepository.findByChatRoomIdAndNotDeletedForUser(chatRoomId, currentUserId);
//
//                log.info("Found {} messages for chat room: {}", messages.size(), chatRoomId);
//
//                // Debug: Check file attachments
//                for (ChatMessage message : messages) {
//                    log.info("Message ID: {}, File Attachment: {}",
//                            message.getId(),
//                            message.getFileAttachment() != null ? message.getFileAttachment().getId() : "NULL");
//                }
//
//                // Mark messages as delivered
//                List<Long> undeliveredMessageIds = messages.stream()
//                        .filter(msg -> msg.getReceiverId().equals(currentUserId) && msg.getStatus() == MessageStatus.SENT)
//                        .map(ChatMessage::getId)
//                        .collect(Collectors.toList());
//
//                if (!undeliveredMessageIds.isEmpty()) {
//                    messageRepository.updateMessageStatus(undeliveredMessageIds, MessageStatus.DELIVERED);
//                }
//
//                return messages.stream()
//                        .map(msg -> convertToResponse(msg, currentUserId))
//                        .collect(Collectors.toList());
//
//            } catch (Exception e) {
//                log.error("Error fetching chat history between {} and {}", currentUserId, otherUserId, e);
//                throw new RuntimeException("Failed to fetch chat history: " + e.getMessage());
//            }
//        }
//
//        public List<ChatRoomResponse> getChatRooms(String employeeId) {
//            try {
//                List<ChatRoom> chatRooms = chatRoomRepository.findAllByEmployeeId(employeeId);
//
//                return chatRooms.stream()
//                        .map(chatRoom -> convertToChatRoomResponse(chatRoom, employeeId))
//                        .collect(Collectors.toList());
//
//            } catch (Exception e) {
//                log.error("Error fetching chat rooms for user {}", employeeId, e);
//                throw new RuntimeException("Failed to fetch chat rooms: " + e.getMessage());
//            }
//        }
//
//        public void markMessagesAsRead(String currentUserId, String otherUserId) {
//            try {
//                String chatRoomId = generateChatRoomId(currentUserId, otherUserId);
//                List<ChatMessage> messages = messageRepository.findByChatRoomId(chatRoomId);
//
//                List<ChatMessage> unreadMessages = messages.stream()
//                        .filter(msg -> msg.getReceiverId().equals(currentUserId) &&
//                                msg.getStatus() == MessageStatus.SENT)
//                        .collect(Collectors.toList());
//
//                if (!unreadMessages.isEmpty()) {
//                    List<Long> messageIds = unreadMessages.stream()
//                            .map(ChatMessage::getId)
//                            .collect(Collectors.toList());
//
//                    messageRepository.updateMessageStatus(messageIds, MessageStatus.READ);
//
//                    // Notify sender about read status via WebSocket
//                    unreadMessages.forEach(msg -> {
//                        webSocketService.sendMessageStatusUpdate(msg.getId(), msg.getSenderId(), MessageStatus.READ);
//                    });
//
//                    // Send read receipt notifications
//                    unreadMessages.forEach(msg -> {
//                        notificationService.sendReadReceiptNotification(currentUserId, msg.getSenderId(), msg.getId());
//                    });
//
//                    log.info("Marked {} messages as read for user {}", unreadMessages.size(), currentUserId);
//                }
//
//            } catch (Exception e) {
//                log.error("Error marking messages as read for user {}", currentUserId, e);
//                throw new RuntimeException("Failed to mark messages as read: " + e.getMessage());
//            }
//        }
//
//        // Helper methods
//        private String generateChatRoomId(String emp1, String emp2) {
//            // String comparison for sorting
//            int comparison = emp1.compareTo(emp2);
//            if (comparison < 0) {
//                return emp1 + "_" + emp2;
//            } else {
//                return emp2 + "_" + emp1;
//            }
//        }
//
//        private ChatRoom getOrCreateChatRoom(String participant1Id, String participant2Id, String chatRoomId) {
//            return chatRoomRepository.findChatRoomBetweenEmployees(participant1Id, participant2Id)
//                    .orElseGet(() -> {
//                        ChatRoom newChatRoom = new ChatRoom();
//                        newChatRoom.setId(chatRoomId);
//
//                        // Sort participants for consistent storage
//                        int comparison = participant1Id.compareTo(participant2Id);
//                        if (comparison < 0) {
//                            newChatRoom.setParticipant1Id(participant1Id);
//                            newChatRoom.setParticipant2Id(participant2Id);
//                        } else {
//                            newChatRoom.setParticipant1Id(participant2Id);
//                            newChatRoom.setParticipant2Id(participant1Id);
//                        }
//
//                        return chatRoomRepository.save(newChatRoom);
//                    });
//        }
//
//        private MessageType getMessageTypeFromFile(String contentType) {
//            if (contentType == null) return MessageType.FILE;
//
//            if (contentType.startsWith("image/")) {
//                return MessageType.IMAGE;
//            } else {
//                return MessageType.FILE;
//            }
//        }
//
//        private ChatMessageResponse convertToResponse(ChatMessage message, String currentUserId) {
//            ChatMessageResponse response = new ChatMessageResponse();
//            response.setId(message.getId());
//            response.setChatRoomId(message.getChatRoomId());
//            response.setSenderId(message.getSenderId());
//            response.setReceiverId(message.getReceiverId());
//            response.setContent(message.getContent());
//            response.setMessageType(message.getMessageType());
//            response.setStatus(message.getStatus());
//            response.setCreatedAt(message.getCreatedAt());
//            response.setDeletedForCurrentUser(message.getDeletedForUsers().contains(currentUserId));
//
//            // DEBUG LOG
//            log.info("Converting message {} - File Attachment: {}",
//                    message.getId(),
//                    message.getFileAttachment() != null ? message.getFileAttachment().getId() : "NULL");
//
//            // Add file attachment if present
//            if (message.getFileAttachment() != null) {
//                FileAttachmentDTO fileDto = new FileAttachmentDTO();
//                fileDto.setId(message.getFileAttachment().getId());
//                fileDto.setFileName(message.getFileAttachment().getFileName());
//                fileDto.setFileUrl(message.getFileAttachment().getFileUrl());
//                fileDto.setFileType(message.getFileAttachment().getFileType());
//                fileDto.setFileSize(message.getFileAttachment().getFileSize());
//                fileDto.setUploadedAt(message.getFileAttachment().getUploadedAt() != null ?
//                        message.getFileAttachment().getUploadedAt().toString() : null);
//
//                response.setFileAttachment(fileDto);
//                log.info("File attachment set for message {}: {}", message.getId(), fileDto.getFileName());
//            } else {
//                log.warn("File attachment is NULL for message {}", message.getId());
//            }
//
//            // Add employee details
//            response.setSenderDetails(employeeService.getEmployeeMeta(message.getSenderId()));
//            response.setReceiverDetails(employeeService.getEmployeeMeta(message.getReceiverId()));
//
//            return response;
//        }
//
//        private ChatRoomResponse convertToChatRoomResponse(ChatRoom chatRoom, String currentUserId) {
//            ChatRoomResponse response = new ChatRoomResponse();
//            response.setId(chatRoom.getId());
//            response.setParticipant1Id(chatRoom.getParticipant1Id());
//            response.setParticipant2Id(chatRoom.getParticipant2Id());
//            response.setUpdatedAt(chatRoom.getUpdatedAt());
//
//            // Set participant details
//            response.setParticipant1Details(employeeService.getEmployeeMeta(chatRoom.getParticipant1Id()));
//            response.setParticipant2Details(employeeService.getEmployeeMeta(chatRoom.getParticipant2Id()));
//
//            // Get last message
//            Optional<ChatMessage> lastMessage = messageRepository.findLastMessageByChatRoomId(chatRoom.getId());
//            lastMessage.ifPresent(message ->
//                    response.setLastMessage(convertToResponse(message, currentUserId)));
//
//            // Get unread count
//            Long unreadCount = messageRepository.countUnreadMessagesInChatRoom(chatRoom.getId(), currentUserId);
//            response.setUnreadCount(unreadCount);
//
//            return response;
//        }
//
//        public ChatMessageResponse sendMessageAndGetResponse(SendMessageRequest request, String senderId) {
//            ChatMessage message = sendMessage(request, senderId);
//            return convertToResponse(message, senderId);
//        }
//    }

package com.erp.chat_service.service;

import com.erp.chat_service.dto.ChatMessageResponse;
import com.erp.chat_service.dto.ChatRoomResponse;
import com.erp.chat_service.dto.FileAttachmentDTO;
import com.erp.chat_service.dto.SendMessageRequest;
import com.erp.chat_service.entity.*;
import com.erp.chat_service.events.MessageSentEvent;
import com.erp.chat_service.repository.ChatMessageRepository;
import com.erp.chat_service.repository.ChatRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ChatService {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Persist-first-then-publish pattern:
     * - save message in DB (transaction)
     * - publish MessageSentEvent (will be handled AFTER_COMMIT by listener)
     */
    public ChatMessage sendMessage(SendMessageRequest request, String senderId) {
        try {
            log.info("=== SEND MESSAGE === from {} to {}", senderId, request.getReceiverId());
            log.debug("File present: {}", request.getFile() != null);

            // Get or create chat room
            String chatRoomId = generateChatRoomId(senderId, request.getReceiverId());
            ChatRoom chatRoom = getOrCreateChatRoom(senderId, request.getReceiverId(), chatRoomId);

            // Handle file upload if present
            FileAttachment fileAttachment = null;
            MessageType messageType = request.getMessageType();

            if (request.getFile() != null && !request.getFile().isEmpty()) {
                log.debug("Uploading file...");
                try {
                    fileAttachment = fileStorageService.uploadFile(request.getFile());
                    messageType = getMessageTypeFromFile(request.getFile().getContentType());
                    log.debug("File uploaded. attachmentId={}", fileAttachment != null ? fileAttachment.getId() : "null");
                } catch (Exception e) {
                    log.error("File upload failed, continuing without attachment: {}", e.getMessage());
                    fileAttachment = null;
                    messageType = MessageType.FILE;
                }
            }

            // Create and save message
            ChatMessage message = new ChatMessage();
            message.setChatRoomId(chatRoomId);
            message.setSenderId(senderId);
            message.setReceiverId(request.getReceiverId());
            message.setContent(request.getContent());
            message.setMessageType(messageType);
            message.setFileAttachment(fileAttachment);
            message.setStatus(MessageStatus.SENT);

            ChatMessage savedMessage = messageRepository.save(message);
            log.info("Message saved id={} chatRoom={}", savedMessage.getId(), chatRoomId);

            // Publish event (will be handled AFTER transaction commit)
            eventPublisher.publishEvent(new MessageSentEvent(savedMessage, request.getReceiverId()));

            return savedMessage;

        } catch (Exception e) {
            log.error("Error sending message from {} to {}: {}", senderId, request.getReceiverId(), e.getMessage(), e);
            throw new RuntimeException("Failed to send message: " + e.getMessage(), e);
        }
    }

    public void deleteMessageForUser(Long messageId, String employeeId) {
        try {
            ChatMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found"));

            if (!message.getSenderId().equals(employeeId) && !message.getReceiverId().equals(employeeId)) {
                throw new IllegalArgumentException("You can only delete your own messages");
            }

            message.getDeletedForUsers().add(employeeId);
            messageRepository.save(message);
            log.info("Message {} deleted for user {}", messageId, employeeId);

        } catch (Exception e) {
            log.error("Error deleting message {} for user {}: {}", messageId, employeeId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete message: " + e.getMessage(), e);
        }
    }

    public List<ChatMessageResponse> getChatHistory(String currentUserId, String otherUserId) {
        try {
            String chatRoomId = generateChatRoomId(currentUserId, otherUserId);

            // Use repository method which excludes messages deleted for current user
            List<ChatMessage> messages = messageRepository.findByChatRoomIdAndNotDeletedForUser(chatRoomId, currentUserId);

            log.debug("Found {} messages for chatRoom {}", messages.size(), chatRoomId);

            // Mark messages as delivered
            List<Long> undeliveredMessageIds = messages.stream()
                    .filter(msg -> msg.getReceiverId().equals(currentUserId) && msg.getStatus() == MessageStatus.SENT)
                    .map(ChatMessage::getId)
                    .collect(Collectors.toList());

            if (!undeliveredMessageIds.isEmpty()) {
                messageRepository.updateMessageStatus(undeliveredMessageIds, MessageStatus.DELIVERED);
            }

            return messages.stream()
                    .map(msg -> convertToResponse(msg, currentUserId))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching chat history between {} and {}: {}", currentUserId, otherUserId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch chat history: " + e.getMessage(), e);
        }
    }

    public List<ChatRoomResponse> getChatRooms(String employeeId) {
        try {
            List<ChatRoom> chatRooms = chatRoomRepository.findAllByEmployeeId(employeeId);

            return chatRooms.stream()
                    .map(chatRoom -> convertToChatRoomResponse(chatRoom, employeeId))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching chat rooms for user {}: {}", employeeId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch chat rooms: " + e.getMessage(), e);
        }
    }

    public void markMessagesAsRead(String currentUserId, String otherUserId) {
        try {
            String chatRoomId = generateChatRoomId(currentUserId, otherUserId);
            List<ChatMessage> messages = messageRepository.findByChatRoomId(chatRoomId);

            List<ChatMessage> unreadMessages = messages.stream()
                    .filter(msg -> msg.getReceiverId().equals(currentUserId) &&
                            msg.getStatus() == MessageStatus.SENT)
                    .collect(Collectors.toList());

            if (!unreadMessages.isEmpty()) {
                List<Long> messageIds = unreadMessages.stream()
                        .map(ChatMessage::getId)
                        .collect(Collectors.toList());

                messageRepository.updateMessageStatus(messageIds, MessageStatus.READ);

                // Notify sender about read status via events or direct websocket (listener can handle)
                unreadMessages.forEach(msg -> {
                    // publish event or directly call notification if needed; here we directly notify via NotificationService
                    try {
                        notificationService.sendReadReceiptNotification(currentUserId, msg.getSenderId(), msg.getId());
                    } catch (Exception e) {
                        log.warn("Failed to send read receipt notification: {}", e.getMessage());
                    }
                });

                log.info("Marked {} messages as read for user {}", unreadMessages.size(), currentUserId);
            }

        } catch (Exception e) {
            log.error("Error marking messages as read for user {}: {}", currentUserId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark messages as read: " + e.getMessage(), e);
        }
    }

    // Helper methods

    private String generateChatRoomId(String emp1, String emp2) {
        int comparison = emp1.compareTo(emp2);
        if (comparison < 0) {
            return emp1 + "_" + emp2;
        } else {
            return emp2 + "_" + emp1;
        }
    }

    private ChatRoom getOrCreateChatRoom(String participant1Id, String participant2Id, String chatRoomId) {
        return chatRoomRepository.findChatRoomBetweenEmployees(participant1Id, participant2Id)
                .orElseGet(() -> {
                    ChatRoom newChatRoom = new ChatRoom();
                    newChatRoom.setId(chatRoomId);

                    int comparison = participant1Id.compareTo(participant2Id);
                    if (comparison < 0) {
                        newChatRoom.setParticipant1Id(participant1Id);
                        newChatRoom.setParticipant2Id(participant2Id);
                    } else {
                        newChatRoom.setParticipant1Id(participant2Id);
                        newChatRoom.setParticipant2Id(participant1Id);
                    }

                    return chatRoomRepository.save(newChatRoom);
                });
    }

    private MessageType getMessageTypeFromFile(String contentType) {
        if (contentType == null) return MessageType.FILE;
        if (contentType.startsWith("image/")) return MessageType.IMAGE;
        return MessageType.FILE;
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

        if (message.getFileAttachment() != null) {
            FileAttachmentDTO fileDto = new FileAttachmentDTO();
            fileDto.setId(message.getFileAttachment().getId());
            fileDto.setFileName(message.getFileAttachment().getFileName());
            fileDto.setFileUrl(message.getFileAttachment().getFileUrl());
            fileDto.setFileType(message.getFileAttachment().getFileType());
            fileDto.setFileSize(message.getFileAttachment().getFileSize());
            fileDto.setUploadedAt(message.getFileAttachment().getUploadedAt() != null ?
                    message.getFileAttachment().getUploadedAt().toString() : null);
            response.setFileAttachment(fileDto);
        }

        response.setSenderDetails(employeeService.getEmployeeMeta(message.getSenderId()));
        response.setReceiverDetails(employeeService.getEmployeeMeta(message.getReceiverId()));

        return response;
    }

    private ChatRoomResponse convertToChatRoomResponse(ChatRoom chatRoom, String currentUserId) {
        ChatRoomResponse response = new ChatRoomResponse();
        response.setId(chatRoom.getId());
        response.setParticipant1Id(chatRoom.getParticipant1Id());
        response.setParticipant2Id(chatRoom.getParticipant2Id());
        response.setUpdatedAt(chatRoom.getUpdatedAt());

        response.setParticipant1Details(employeeService.getEmployeeMeta(chatRoom.getParticipant1Id()));
        response.setParticipant2Details(employeeService.getEmployeeMeta(chatRoom.getParticipant2Id()));

        Optional<ChatMessage> lastMessage = messageRepository.findFirstByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId());
        lastMessage.ifPresent(message -> response.setLastMessage(convertToResponse(message, currentUserId)));

        Long unreadCount = messageRepository.countUnreadMessagesInChatRoom(chatRoom.getId(), currentUserId);
        response.setUnreadCount(unreadCount);

        return response;
    }

    public ChatMessageResponse sendMessageAndGetResponse(SendMessageRequest request, String senderId) {
        ChatMessage message = sendMessage(request, senderId);
        return convertToResponse(message, senderId);
    }
}
