package com.erp.chat_service.dto;

import com.erp.chat_service.entity.MessageStatus;
import com.erp.chat_service.entity.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private String chatRoomId;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageType messageType;
    private FileAttachmentDTO fileAttachment;
    private MessageStatus status;
    private ZonedDateTime createdAt;
    private boolean deletedForCurrentUser;
    private EmployeeDTO senderDetails;
    private EmployeeDTO receiverDetails;
}