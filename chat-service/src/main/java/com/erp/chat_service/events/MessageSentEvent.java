package com.erp.chat_service.events;

import com.erp.chat_service.entity.ChatMessage;

public class MessageSentEvent {
    private final ChatMessage message;
    private final String receiverId;

    public MessageSentEvent(ChatMessage message, String receiverId) {
        this.message = message;
        this.receiverId = receiverId;
    }

    public ChatMessage getMessage() { return message; }
    public String getReceiverId() { return receiverId; }
}
