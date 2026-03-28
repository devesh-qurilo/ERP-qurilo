package com.erp.project_service.mapper;

import com.erp.project_service.dto.Discussion.DiscussionMessageResponse;
import com.erp.project_service.entity.DiscussionMessage;
import com.erp.project_service.entity.DiscussionRoom;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DiscussionMessageMapper {

    public DiscussionMessageResponse toResponse(DiscussionMessage message) {
        if (message == null) {
            return null;
        }

        return DiscussionMessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .roomId(message.getRoom().getId())
                .parentMessageId(message.getParentMessage() != null ? message.getParentMessage().getId() : null)
                .senderId(message.getSenderId())
                .messageType(message.getMessageType())
                .filePath(message.getFilePath())
                .fileUrl(message.getFileUrl())
                .fileName(message.getFileName())
                .fileSize(message.getFileSize())
                .mimeType(message.getMimeType())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .isDeleted(message.getIsDeleted())
                .deletedBy(message.getDeletedBy())
                .isBestReply(message.getIsBestReply()) // NEW FIELD
                .replyCount(0) // Will be set separately
                .build();
    }

    public DiscussionMessageResponse toResponseWithReplies(DiscussionMessage message, List<DiscussionMessageResponse> replies) {
        DiscussionMessageResponse response = toResponse(message);
        if (response != null) {
            response.setReplies(replies);
            response.setReplyCount(replies != null ? replies.size() : 0);
        }
        return response;
    }

    public DiscussionMessage toEntity(String content, DiscussionRoom room, DiscussionMessage parentMessage,
                                      String senderId, DiscussionMessage.MessageType messageType) {
        return DiscussionMessage.builder()
                .content(content)
                .room(room)
                .parentMessage(parentMessage)
                .senderId(senderId)
                .messageType(messageType)
                .isDeleted(false)
                .build();
    }

    public DiscussionMessage toFileEntity(DiscussionRoom room, String senderId, String fileName,
                                          String filePath, String fileUrl, Long fileSize, String mimeType) {
        return DiscussionMessage.builder()
                .content("Shared a file: " + fileName)
                .room(room)
                .senderId(senderId)
                .messageType(DiscussionMessage.MessageType.FILE)
                .fileName(fileName)
                .filePath(filePath)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .mimeType(mimeType)
                .isDeleted(false)
                .build();
    }
}