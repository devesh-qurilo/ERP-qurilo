package com.erp.project_service.websocket;

import com.erp.project_service.dto.Discussion.DiscussionMessageRequest;
import com.erp.project_service.dto.Discussion.DiscussionMessageResponse;
import com.erp.project_service.dto.Discussion.DiscussionRoomRequest;
import com.erp.project_service.dto.Discussion.DiscussionRoomResponse;
import com.erp.project_service.service.interfaces.DiscussionRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DiscussionWebSocketController {

    private final DiscussionRoomService discussionRoomService;

    /**
     * ✅ Room create via WebSocket
     *
     * Client:
     *  destination: /app/projects/{projectId}/discussion-rooms/create
     *  body: DiscussionRoomRequest (JSON)
     */
    @MessageMapping("/projects/{projectId}/discussion-rooms/create")
    public void createRoom(@DestinationVariable Long projectId,
                           DiscussionRoomRequest request,
                           Principal principal) {

        String userId = principal != null ? principal.getName() : "UNKNOWN";
        log.info("WS createRoom projectId={}, user={}", projectId, userId);

        // REST me jo logic hai, wohi service call
        DiscussionRoomResponse response =
                discussionRoomService.createRoom(projectId, request, userId);

        // Broadcast already DiscussionRoomServiceImpl.notifyRoomUpdate() se ho rahi hai
    }

    /**
     * ✅ Send message (text + optional parentMessageId)
     *
     * Client:
     *  destination: /app/discussion-rooms/{roomId}/messages/send
     *  body: DiscussionMessageRequest (JSON) [file WS se nahi aayega]
     */
    @MessageMapping("/discussion-rooms/{roomId}/messages/send")
    public void sendMessage(@DestinationVariable Long roomId,
                            DiscussionMessageRequest request,
                            Principal principal) {

        String userId = principal != null ? principal.getName() : "UNKNOWN";
        log.info("WS sendMessage roomId={}, user={}", roomId, userId);

        // Yaha request.getFile() hamesha null rahega (WS se file nahi aati),
        // lekin text + parentMessageId wala pura logic reuse hoga.
        DiscussionMessageResponse response =
                discussionRoomService.sendMessage(roomId, request, userId);

        // Service khud hi NEW_MESSAGE broadcast kar rahi hai
    }

    /**
     * ✅ Reply to a message
     *
     * destination:
     *  /app/discussion-rooms/{roomId}/messages/{parentMessageId}/reply
     */
    @MessageMapping("/discussion-rooms/{roomId}/messages/{parentMessageId}/reply")
    public void replyToMessage(@DestinationVariable Long roomId,
                               @DestinationVariable Long parentMessageId,
                               DiscussionMessageRequest request,
                               Principal principal) {

        String userId = principal != null ? principal.getName() : "UNKNOWN";
        log.info("WS replyToMessage roomId={}, parentMessageId={}, user={}",
                roomId, parentMessageId, userId);

        request.setParentMessageId(parentMessageId);
        discussionRoomService.replyToMessage(parentMessageId, request, userId);
    }

    /**
     * ✅ Update message (sirf content update)
     *
     * destination:
     *  /app/discussion-rooms/{roomId}/messages/{messageId}/update
     * body:
     *  plain String (JSON string bhi chalega)
     */
    @MessageMapping("/discussion-rooms/{roomId}/messages/{messageId}/update")
    public void updateMessage(@DestinationVariable Long roomId,
                              @DestinationVariable Long messageId,
                              String content,
                              Principal principal) {

        String userId = principal != null ? principal.getName() : "UNKNOWN";
        log.info("WS updateMessage roomId={}, messageId={}, user={}",
                roomId, messageId, userId);

        discussionRoomService.updateMessage(messageId, content, userId);
    }

    /**
     * ✅ Delete message
     *
     * destination:
     *  /app/discussion-rooms/{roomId}/messages/{messageId}/delete
     */
    @MessageMapping("/discussion-rooms/{roomId}/messages/{messageId}/delete")
    public void deleteMessage(@DestinationVariable Long roomId,
                              @DestinationVariable Long messageId,
                              Principal principal) {

        String userId = principal != null ? principal.getName() : "UNKNOWN";
        log.info("WS deleteMessage roomId={}, messageId={}, user={}",
                roomId, messageId, userId);

        discussionRoomService.deleteMessage(messageId, userId);
        // Service khud "DELETE_MESSAGE" type ka RealTimeMessage broadcast kar rahi hai
    }

    /**
     * ✅ Mark best reply
     *
     * destination:
     *  /app/discussion-rooms/{roomId}/messages/{messageId}/mark-best
     */
    @MessageMapping("/discussion-rooms/{roomId}/messages/{messageId}/mark-best")
    public void markBestReply(@DestinationVariable Long roomId,
                              @DestinationVariable Long messageId,
                              Principal principal) {

        String userId = principal != null ? principal.getName() : "UNKNOWN";
        log.info("WS markBestReply roomId={}, messageId={}, user={}",
                roomId, messageId, userId);

        discussionRoomService.markAsBestReply(messageId, userId);
    }

    /**
     * ✅ Unmark best reply
     *
     * destination:
     *  /app/discussion-rooms/{roomId}/messages/{messageId}/unmark-best
     */
    @MessageMapping("/discussion-rooms/{roomId}/messages/{messageId}/unmark-best")
    public void unmarkBestReply(@DestinationVariable Long roomId,
                                @DestinationVariable Long messageId,
                                Principal principal) {

        String userId = principal != null ? principal.getName() : "UNKNOWN";
        log.info("WS unmarkBestReply roomId={}, messageId={}, user={}",
                roomId, messageId, userId);

        discussionRoomService.unmarkBestReply(messageId, userId);
    }
}
