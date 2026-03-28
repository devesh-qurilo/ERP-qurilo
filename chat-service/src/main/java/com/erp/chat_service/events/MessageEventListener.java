package com.erp.chat_service.events;

import com.erp.chat_service.service.NotificationService;
import com.erp.chat_service.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
public class MessageEventListener {

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageSent(MessageSentEvent event) {
        try {
            webSocketService.sendMessageToUser(event.getMessage(), event.getReceiverId());
            notificationService.sendMessageNotification(
                    event.getMessage().getSenderId(),
                    event.getReceiverId(),
                    event.getMessage().getContent(),
                    event.getMessage().getFileAttachment() != null
            );
        } catch (Exception e) {
            log.error("Failed to process MessageSentEvent: {}", e.getMessage(), e);
        }
    }
}
