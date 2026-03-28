package com.erp.project_service.service.interfaces;

import com.erp.project_service.dto.Discussion.DiscussionRoomRequest;
import com.erp.project_service.dto.Discussion.DiscussionRoomResponse;
import com.erp.project_service.dto.Discussion.DiscussionMessageRequest;
import com.erp.project_service.dto.Discussion.DiscussionMessageResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DiscussionRoomService {

    // Room Management
    @Transactional
    DiscussionRoomResponse createRoom(Long projectId, DiscussionRoomRequest request, String createdBy);

    // NEW METHOD: Mark message as best reply
    @Transactional
    DiscussionMessageResponse markAsBestReply(Long messageId, String userId);

    // NEW METHOD: Unmark best reply
    @Transactional
    DiscussionMessageResponse unmarkBestReply(Long messageId, String userId);

    List<DiscussionRoomResponse> getRoomsByProject(Long projectId, String requesterId);
    DiscussionRoomResponse getRoomById(Long roomId, String requesterId);
    void deleteRoom(Long roomId, String deletedBy);

    // Message Management
    DiscussionMessageResponse sendMessage(Long roomId, DiscussionMessageRequest request, String senderId);
    DiscussionMessageResponse sendFileMessage(Long roomId, MultipartFile file, String senderId);
    List<DiscussionMessageResponse> getRoomMessages(Long roomId, String requesterId);
    DiscussionMessageResponse getMessage(Long messageId, String requesterId);
    DiscussionMessageResponse updateMessage(Long messageId, String content, String updaterId);
    void deleteMessage(Long messageId, String deleterId);

    // Replies
    DiscussionMessageResponse replyToMessage(Long parentMessageId, DiscussionMessageRequest request, String senderId);
    List<DiscussionMessageResponse> getMessageReplies(Long parentMessageId, String requesterId);
}