package com.erp.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private String id;
    private String participant1Id;
    private String participant2Id;
    private EmployeeDTO participant1Details;
    private EmployeeDTO participant2Details;
    private ChatMessageResponse lastMessage;
    private ZonedDateTime updatedAt;
    private Long unreadCount;
}