package com.erp.chat_service.controller;

import com.erp.chat_service.entity.ChatMessage;
import com.erp.chat_service.repository.ChatMessageRepository;
import com.erp.chat_service.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Optional;

@Slf4j
@Controller
public class WebSocketController {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /**
     * 8. Typing Indicator
     * Frontend Use: Show typing indicator when user is typing
     */
    @MessageMapping("/chat.typing")
    @SendToUser("/queue/typing")
    public WebSocketService.TypingIndicator handleTyping(
            @Payload WebSocketService.TypingIndicator typingIndicator,
            Principal principal) {

        String currentUserId = principal.getName();
        log.debug("User {} typing: {}", currentUserId, typingIndicator.isTyping());

        typingIndicator.setSenderId(currentUserId);
        return typingIndicator;
    }

    /**
     * 9. Online Status
     * Frontend Use: Show online/offline status of users
     */
    @MessageMapping("/chat.online")
    public void handleOnlineStatus(
            @Payload WebSocketService.OnlineStatusUpdate statusUpdate,
            Principal principal) {

        String currentUserId = principal.getName();
        log.debug("User {} online status: {}", currentUserId, statusUpdate.isOnline());

        statusUpdate.setEmployeeId(currentUserId);
        webSocketService.sendOnlineStatus(currentUserId, statusUpdate.isOnline());
    }

    /**
     * 10. Read Receipt
     * Frontend Use: Show when message is read by receiver
     */
    @MessageMapping("/chat.read-receipt")
    public void handleReadReceipt(
            @Payload WebSocketService.MessageStatusUpdate statusUpdate,
            Principal principal) {

        String currentUserId = principal.getName();
        log.debug("User {} marked message {} as read", currentUserId, statusUpdate.getMessageId());

        Optional<ChatMessage> messageOpt = chatMessageRepository.findById(statusUpdate.getMessageId());
        if (messageOpt.isPresent()) {
            ChatMessage message = messageOpt.get();
            if (message.getReceiverId().equals(currentUserId)) {
                webSocketService.sendMessageStatusUpdate(
                        statusUpdate.getMessageId(),
                        message.getSenderId(),
                        statusUpdate.getStatus()
                );
            }
        }
    }
}