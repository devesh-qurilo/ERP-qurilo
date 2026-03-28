package com.erp.project_service.service.impl;

import com.erp.project_service.client.EmployeeClient;
import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.dto.Discussion.*;
import com.erp.project_service.dto.file.FileMetaDto;
import com.erp.project_service.entity.*;
import com.erp.project_service.exception.NotFoundException;
import com.erp.project_service.exception.UnauthorizedException;
import com.erp.project_service.mapper.DiscussionMessageMapper;
import com.erp.project_service.mapper.DiscussionRoomMapper;
import com.erp.project_service.repository.*;
import com.erp.project_service.service.interfaces.DiscussionRoomService;
import com.erp.project_service.service.interfaces.FileService;
import com.erp.project_service.service.interfaces.ProjectActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscussionRoomServiceImpl implements DiscussionRoomService {

    private final DiscussionRoomRepository discussionRoomRepository;
    private final DiscussionMessageRepository discussionMessageRepository;
    private final ProjectRepository projectRepository;
    private final DiscussionCategoryRepository discussionCategoryRepository;
    private final EmployeeClient employeeClient;
    private final FileService fileService;
    private final ProjectActivityService activityService;
    private final SimpMessagingTemplate messagingTemplate;

    private final DiscussionRoomMapper discussionRoomMapper;
    private final DiscussionMessageMapper discussionMessageMapper;

    // Room Management Methods
    @Transactional
    @Override
    public DiscussionRoomResponse createRoom(Long projectId, DiscussionRoomRequest request, String createdBy) {
        // Validate request
        if (request.getInitialMessage() == null || request.getInitialMessage().trim().isEmpty()) {
            throw new IllegalArgumentException("Initial message is required");
        }

        // Validate project exists and user has access
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        validateUserProjectAccess(project, createdBy, "create discussion room");

        // Validate category if provided
        DiscussionCategory category = null;
        if (request.getCategoryId() != null) {
            category = discussionCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Discussion category not found"));
        }

        // Create room
        DiscussionRoom room = discussionRoomMapper.toEntity(request, project, category, createdBy);
        DiscussionRoom savedRoom = discussionRoomRepository.save(room);

        // Create initial message (COMPULSORY)
        DiscussionMessage initialMessage = discussionMessageMapper.toEntity(
                request.getInitialMessage(), savedRoom, null, createdBy, DiscussionMessage.MessageType.TEXT);

        // Handle initial file if provided
        if (request.getInitialFile() != null && !request.getInitialFile().isEmpty()) {
            try {
                FileMetaDto fileMeta = fileService.uploadProjectFile(projectId, request.getInitialFile(), createdBy);
                initialMessage.setMessageType(DiscussionMessage.MessageType.FILE);
                initialMessage.setFilePath(fileMeta.getPath());
                initialMessage.setFileUrl(fileMeta.getUrl());
                initialMessage.setFileName(request.getInitialFile().getOriginalFilename());
                initialMessage.setFileSize(request.getInitialFile().getSize());
                initialMessage.setMimeType(request.getInitialFile().getContentType());
            } catch (Exception e) {
                log.error("Failed to upload initial file for room {}: {}", savedRoom.getId(), e.getMessage());
                // Continue without file - message is still created
            }
        }

        discussionMessageRepository.save(initialMessage);

        // Record activity
        activityService.record(project.getId(), createdBy, "DISCUSSION_ROOM_CREATED", savedRoom.getTitle());

        // Enrich and return response
        DiscussionRoomResponse response = discussionRoomMapper.toResponseWithCategory(savedRoom);
        enrichRoomResponse(response);

        // Notify via WebSocket
        notifyRoomUpdate(savedRoom, "NEW_ROOM", createdBy);

        return response;
    }

    @Override
    @Transactional
    public DiscussionMessageResponse markAsBestReply(Long messageId, String userId) {
        DiscussionMessage message = discussionMessageRepository.findByIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        validateUserProjectAccess(message.getRoom().getProject(), userId, "mark best reply");

        // Only message sender or admin can mark as best reply
        if (!message.getSenderId().equals(userId) && !com.erp.project_service.security.SecurityUtils.isAdmin()) {
            throw new UnauthorizedException("Only message sender or admin can mark as best reply");
        }

        // ✅ FIXED: Check if message is a reply (has parent message)
//        if (message.getParentMessage() == null) {
//            throw new IllegalArgumentException("Only replies can be marked as best reply. This message is not a reply.");
//        }

        Long roomId = message.getRoom().getId();
//        Long parentMessageId = message.getParentMessage().getId(); // ✅ Now safe to call

        // ✅ Check if there's already a best reply in this room
        DiscussionMessage existingBestReply = findExistingBestReplyInRoom(roomId);
        if (existingBestReply != null && !existingBestReply.getId().equals(messageId)) {
            throw new IllegalArgumentException("There is already a best reply in this room. Please unmark it first.");
        }

        // Mark this message as best reply
        message.setIsBestReply(true);
        DiscussionMessage updatedMessage = discussionMessageRepository.save(message);

        // Update room's updatedAt timestamp
        message.getRoom().setUpdatedAt(LocalDateTime.now());
        discussionRoomRepository.save(message.getRoom());

        // Enrich and broadcast update
        DiscussionMessageResponse response = discussionMessageMapper.toResponse(updatedMessage);
        enrichMessageResponse(response);

        notifyMessageUpdate(roomId, response, "BEST_REPLY_MARKED", userId);

        log.info("✅ Message {} marked as best reply in room {}", messageId, roomId);
        return response;
    }

    // ✅ NEW: Helper method to find existing best reply in a room
// ✅ OPTIMIZED: Using repository query
    private DiscussionMessage findExistingBestReplyInRoom(Long roomId) {
        return discussionMessageRepository.findBestReplyByRoomId(roomId)
                .orElse(null);
    }

    // ✅ NEW: Alternative implementation using repository query (more efficient)
    private DiscussionMessage findExistingBestReplyInRoomOptimized(Long roomId) {
        // You can add this method to your DiscussionMessageRepository for better performance
        // @Query("SELECT dm FROM DiscussionMessage dm WHERE dm.room.id = :roomId AND dm.isBestReply = true AND dm.isDeleted = false")
        // Optional<DiscussionMessage> findBestReplyByRoomId(@Param("roomId") Long roomId);

        // For now, using the existing method
        return discussionMessageRepository.findCompleteThreadByRoomId(roomId).stream()
                .filter(msg -> Boolean.TRUE.equals(msg.getIsBestReply()) && !msg.getIsDeleted())
                .findFirst()
                .orElse(null);
    }

    // NEW METHOD: Unmark best reply
    @Transactional
    @Override
    public DiscussionMessageResponse unmarkBestReply(Long messageId, String userId) {
        DiscussionMessage message = discussionMessageRepository.findByIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        validateUserProjectAccess(message.getRoom().getProject(), userId, "unmark best reply");

        // Only message sender or admin can unmark best reply
        if (!message.getSenderId().equals(userId) && !com.erp.project_service.security.SecurityUtils.isAdmin()) {
            throw new UnauthorizedException("Only message sender or admin can unmark best reply");
        }

        if (message.getIsBestReply() == null || !message.getIsBestReply()) {
            throw new IllegalArgumentException("Message is not marked as best reply");
        }

        message.setIsBestReply(false);
        DiscussionMessage updatedMessage = discussionMessageRepository.save(message);

        // Update room's updatedAt timestamp
        message.getRoom().setUpdatedAt(LocalDateTime.now());
        discussionRoomRepository.save(message.getRoom());

        // Enrich and broadcast update
        DiscussionMessageResponse response = discussionMessageMapper.toResponse(updatedMessage);
        enrichMessageResponse(response);

        notifyMessageUpdate(message.getRoom().getId(), response, "BEST_REPLY_UNMARKED", userId);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscussionRoomResponse> getRoomsByProject(Long projectId, String requesterId) {
        // Validate project exists and user has access
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        validateUserProjectAccess(project, requesterId, "view discussion rooms");

        // Get rooms with access control
        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();
        List<DiscussionRoom> rooms = discussionRoomRepository.findAccessibleRoomsByProject(projectId, requesterId, isAdmin);

        return rooms.stream()
                .map(room -> {
                    DiscussionRoomResponse response = discussionRoomMapper.toResponseWithCategory(room);
                    enrichRoomResponse(response);

                    // Add last message and message count
                    List<DiscussionMessage> latestMessages = discussionMessageRepository.findLatestMessagesByRoomId(room.getId());
                    if (!latestMessages.isEmpty()) {
                        response.setLastMessage(discussionMessageMapper.toResponse(latestMessages.get(0)));
                    }

                    Long messageCount = discussionMessageRepository.countMessagesByRoomId(room.getId());
                    response.setMessageCount(messageCount);

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DiscussionRoomResponse getRoomById(Long roomId, String requesterId) {
        DiscussionRoom room = discussionRoomRepository.findByIdAndIsActiveTrue(roomId)
                .orElseThrow(() -> new NotFoundException("Discussion room not found"));

        validateUserProjectAccess(room.getProject(), requesterId, "view discussion room");

        DiscussionRoomResponse response = discussionRoomMapper.toResponseWithCategory(room);
        enrichRoomResponse(response);

        return response;
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId, String deletedBy) {
        DiscussionRoom room = discussionRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Discussion room not found"));

        // Only admin can delete rooms
        if (!com.erp.project_service.security.SecurityUtils.isAdmin()) {
            throw new UnauthorizedException("Only admin can delete discussion rooms");
        }

        // Soft delete the room
        room.setIsActive(false);
        room.setUpdatedAt(LocalDateTime.now());
        discussionRoomRepository.save(room);

        // Record activity
        activityService.record(room.getProject().getId(), deletedBy, "DISCUSSION_ROOM_DELETED", room.getTitle());

        // Notify via WebSocket
        notifyRoomUpdate(room, "ROOM_DELETED", deletedBy);
    }

    // Message Management Methods
    // Updated sendMessage method to handle file + text together
    @Override
    @Transactional
    public DiscussionMessageResponse sendMessage(Long roomId, DiscussionMessageRequest request, String senderId) {
        DiscussionRoom room = discussionRoomRepository.findByIdAndIsActiveTrue(roomId)
                .orElseThrow(() -> new NotFoundException("Discussion room not found"));

        validateUserProjectAccess(room.getProject(), senderId, "send message");

        // Validate that either content or file is present
        if ((request.getContent() == null || request.getContent().trim().isEmpty()) &&
                (request.getFile() == null || request.getFile().isEmpty())) {
            throw new IllegalArgumentException("Either message content or file is required");
        }

        DiscussionMessage parentMessage = null;
        if (request.getParentMessageId() != null) {
            parentMessage = discussionMessageRepository.findByIdAndIsDeletedFalse(request.getParentMessageId())
                    .orElseThrow(() -> new NotFoundException("Parent message not found"));

            // Validate parent message belongs to same room
            if (!parentMessage.getRoom().getId().equals(roomId)) {
                throw new IllegalArgumentException("Parent message does not belong to this room");
            }
        }

        DiscussionMessage.MessageType messageType = DiscussionMessage.MessageType.TEXT;
        String content = request.getContent();

        // Handle file upload if present
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            try {
                FileMetaDto fileMeta = fileService.uploadProjectFile(room.getProject().getId(), request.getFile(), senderId);
                messageType = DiscussionMessage.MessageType.FILE;

                // If no content provided, use default file message
                if (content == null || content.trim().isEmpty()) {
                    content = "Shared a file: " + request.getFile().getOriginalFilename();
                }

                DiscussionMessage message = discussionMessageMapper.toEntity(content, room, parentMessage, senderId, messageType);
                message.setFilePath(fileMeta.getPath());
                message.setFileUrl(fileMeta.getUrl());
                message.setFileName(request.getFile().getOriginalFilename());
                message.setFileSize(request.getFile().getSize());
                message.setMimeType(request.getFile().getContentType());

                DiscussionMessage savedMessage = discussionMessageRepository.save(message);

                // Update room and notify
                room.setUpdatedAt(LocalDateTime.now());
                discussionRoomRepository.save(room);

                DiscussionMessageResponse response = discussionMessageMapper.toResponse(savedMessage);
                enrichMessageResponse(response);

                notifyMessageUpdate(roomId, response, "NEW_MESSAGE", senderId);

                return response;

            } catch (Exception e) {
                log.error("Failed to upload file for message in room {}: {}", roomId, e.getMessage());
                throw new RuntimeException("File upload failed: " + e.getMessage());
            }
        }

        // Text-only message
        DiscussionMessage message = discussionMessageMapper.toEntity(content, room, parentMessage, senderId, messageType);
        DiscussionMessage savedMessage = discussionMessageRepository.save(message);

        room.setUpdatedAt(LocalDateTime.now());
        discussionRoomRepository.save(room);

        DiscussionMessageResponse response = discussionMessageMapper.toResponse(savedMessage);
        enrichMessageResponse(response);

        notifyMessageUpdate(roomId, response, "NEW_MESSAGE", senderId);

        return response;
    }


    @Override
    @Transactional
    public DiscussionMessageResponse sendFileMessage(Long roomId, MultipartFile file, String senderId) {
        DiscussionRoom room = discussionRoomRepository.findByIdAndIsActiveTrue(roomId)
                .orElseThrow(() -> new NotFoundException("Discussion room not found"));

        validateUserProjectAccess(room.getProject(), senderId, "send file");

        try {
            // Upload file using existing file service
            FileMetaDto fileMeta = fileService.uploadProjectFile(room.getProject().getId(), file, senderId);

            // Create file message
            DiscussionMessage message = discussionMessageMapper.toFileEntity(
                    room, senderId, file.getOriginalFilename(),
                    fileMeta.getPath(), fileMeta.getUrl(), file.getSize(), file.getContentType());

            DiscussionMessage savedMessage = discussionMessageRepository.save(message);

            // Update room's updatedAt timestamp
            room.setUpdatedAt(LocalDateTime.now());
            discussionRoomRepository.save(room);

            // Record activity
            activityService.record(room.getProject().getId(), senderId, "DISCUSSION_FILE_SENT",
                    String.format("File: %s in room: %s", file.getOriginalFilename(), room.getTitle()));

            // Enrich and broadcast message
            DiscussionMessageResponse response = discussionMessageMapper.toResponse(savedMessage);
            enrichMessageResponse(response);

            notifyMessageUpdate(roomId, response, "NEW_MESSAGE", senderId);

            return response;

        } catch (Exception e) {
            log.error("Failed to upload file for discussion room {}: {}", roomId, e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscussionMessageResponse> getRoomMessages(Long roomId, String requesterId) {
        DiscussionRoom room = discussionRoomRepository.findByIdAndIsActiveTrue(roomId)
                .orElseThrow(() -> new NotFoundException("Discussion room not found"));

        validateUserProjectAccess(room.getProject(), requesterId, "view messages");

        // Get main messages (not replies)
        List<DiscussionMessage> mainMessages = discussionMessageRepository
                .findByRoomIdAndParentMessageIsNullAndIsDeletedFalseOrderByCreatedAtDesc(roomId);

        return mainMessages.stream()
                .map(message -> {
                    DiscussionMessageResponse response = discussionMessageMapper.toResponse(message);
                    enrichMessageResponse(response);

                    // Get replies count
                    Long replyCount = discussionMessageRepository.countRepliesByParentId(message.getId(), roomId);
                    response.setReplyCount(replyCount.intValue());

                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DiscussionMessageResponse getMessage(Long messageId, String requesterId) {
        DiscussionMessage message = discussionMessageRepository.findByIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new NotFoundException("Message not found"));

        validateUserProjectAccess(message.getRoom().getProject(), requesterId, "view message");

        DiscussionMessageResponse response = discussionMessageMapper.toResponse(message);
        enrichMessageResponse(response);

        // Get replies
        List<DiscussionMessage> replies = discussionMessageRepository
                .findByParentMessageIdAndIsDeletedFalseOrderByCreatedAtAsc(messageId);

        List<DiscussionMessageResponse> replyResponses = replies.stream()
                .map(reply -> {
                    DiscussionMessageResponse replyResponse = discussionMessageMapper.toResponse(reply);
                    enrichMessageResponse(replyResponse);
                    return replyResponse;
                })
                .collect(Collectors.toList());

        response.setReplies(replyResponses);
        response.setReplyCount(replyResponses.size());

        return response;
    }

    @Override
    @Transactional
    public DiscussionMessageResponse updateMessage(Long messageId, String content, String updaterId) {
        DiscussionMessage message = discussionMessageRepository
                .findByIdAndSenderOrAdmin(messageId, updaterId, com.erp.project_service.security.SecurityUtils.isAdmin())
                .orElseThrow(() -> new NotFoundException("Message not found or you don't have permission to edit"));

        // Check if message is deleted
        if (message.getIsDeleted()) {
            throw new NotFoundException("Message not found");
        }

        // Only owner can edit their own message
        if (!message.getSenderId().equals(updaterId)) {
            throw new UnauthorizedException("You can only edit your own messages");
        }

        // Update message content
        message.setContent(content);
        message.setUpdatedAt(LocalDateTime.now());

        DiscussionMessage updatedMessage = discussionMessageRepository.save(message);

        // Update room's updatedAt timestamp
        updatedMessage.getRoom().setUpdatedAt(LocalDateTime.now());
        discussionRoomRepository.save(updatedMessage.getRoom());

        // Enrich and broadcast update
        DiscussionMessageResponse response = discussionMessageMapper.toResponse(updatedMessage);
        enrichMessageResponse(response);

        notifyMessageUpdate(updatedMessage.getRoom().getId(), response, "UPDATE_MESSAGE", updaterId);

        return response;
    }

    @Override
    @Transactional
    public void deleteMessage(Long messageId, String deleterId) {
        DiscussionMessage message = discussionMessageRepository
                .findByIdAndSenderOrAdmin(messageId, deleterId, com.erp.project_service.security.SecurityUtils.isAdmin())
                .orElseThrow(() -> new NotFoundException("Message not found or you don't have permission to delete"));

        // Only owner can delete their own message
        if (!com.erp.project_service.security.SecurityUtils.isAdmin() && !message.getSenderId().equals(deleterId)) {
            throw new UnauthorizedException("You can only delete your own messages");
        }

        Long roomId = message.getRoom().getId();

        // PERMANENTLY DELETE the message (not soft delete)
        discussionMessageRepository.delete(message);

        // If it's a file message, also delete the file from storage
        if (message.getMessageType() == DiscussionMessage.MessageType.FILE && message.getFilePath() != null) {
            try {
                // Find the file meta and delete it
                // This assumes you have a way to find FileMeta by path or URL
                // You might need to add this method to your FileService
            } catch (Exception e) {
                log.warn("Failed to delete file from storage for message {}: {}", messageId, e.getMessage());
            }
        }

        // Update room's updatedAt timestamp
        message.getRoom().setUpdatedAt(LocalDateTime.now());
        discussionRoomRepository.save(message.getRoom());

        // Record activity
        activityService.record(message.getRoom().getProject().getId(), deleterId,
                "DISCUSSION_MESSAGE_DELETED", "Message permanently deleted");

        // Notify deletion via WebSocket
        RealTimeMessage deleteNotification = RealTimeMessage.builder()
                .type("DELETE_MESSAGE")
                .roomId(roomId)
                .message(DiscussionMessageResponse.builder().id(messageId).build()) // Only send ID
                .actionBy(deleterId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend("/topic/discussion/room/" + roomId, deleteNotification);
    }

    // Reply Methods
    @Override
    @Transactional
    public DiscussionMessageResponse replyToMessage(Long parentMessageId, DiscussionMessageRequest request, String senderId) {
        DiscussionMessage parentMessage = discussionMessageRepository.findByIdAndIsDeletedFalse(parentMessageId)
                .orElseThrow(() -> new NotFoundException("Parent message not found"));

        validateUserProjectAccess(parentMessage.getRoom().getProject(), senderId, "reply to message");

        DiscussionMessage reply = discussionMessageMapper.toEntity(
                request.getContent(), parentMessage.getRoom(), parentMessage, senderId, DiscussionMessage.MessageType.TEXT);

        DiscussionMessage savedReply = discussionMessageRepository.save(reply);

        // Update room's updatedAt timestamp
        parentMessage.getRoom().setUpdatedAt(LocalDateTime.now());
        discussionRoomRepository.save(parentMessage.getRoom());

        // Enrich and broadcast reply
        DiscussionMessageResponse response = discussionMessageMapper.toResponse(savedReply);
        enrichMessageResponse(response);

        notifyMessageUpdate(parentMessage.getRoom().getId(), response, "NEW_MESSAGE", senderId);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiscussionMessageResponse> getMessageReplies(Long parentMessageId, String requesterId) {
        DiscussionMessage parentMessage = discussionMessageRepository.findByIdAndIsDeletedFalse(parentMessageId)
                .orElseThrow(() -> new NotFoundException("Parent message not found"));

        validateUserProjectAccess(parentMessage.getRoom().getProject(), requesterId, "view replies");

        List<DiscussionMessage> replies = discussionMessageRepository
                .findByParentMessageIdAndIsDeletedFalseOrderByCreatedAtAsc(parentMessageId);

        return replies.stream()
                .map(reply -> {
                    DiscussionMessageResponse response = discussionMessageMapper.toResponse(reply);
                    enrichMessageResponse(response);
                    return response;
                })
                .collect(Collectors.toList());
    }

    // Helper Methods
    private void validateUserProjectAccess(Project project, String userId, String action) {
        boolean isAdmin = com.erp.project_service.security.SecurityUtils.isAdmin();

        if (!isAdmin && (project.getAssignedEmployeeIds() == null ||
                !project.getAssignedEmployeeIds().contains(userId))) {
            throw new UnauthorizedException("You don't have permission to " + action + " in this project");
        }
    }

    private void enrichRoomResponse(DiscussionRoomResponse response) {
        if (response == null) return;

        // Enrich createdBy user data
        try {
            EmployeeMetaDto creator = employeeClient.getMeta(response.getCreatedBy());
            response.setCreatedByUser(creator);
        } catch (Exception e) {
            log.warn("Failed to fetch creator data for user {}: {}", response.getCreatedBy(), e.getMessage());
        }
    }

    private void enrichMessageResponse(DiscussionMessageResponse response) {
        if (response == null) return;

        // Enrich sender user data
        try {
            EmployeeMetaDto sender = employeeClient.getMeta(response.getSenderId());
            response.setSender(sender);
        } catch (Exception e) {
            log.warn("Failed to fetch sender data for user {}: {}", response.getSenderId(), e.getMessage());
        }
    }

    private void notifyRoomUpdate(DiscussionRoom room, String actionType, String actionBy) {
        try {
            DiscussionRoomResponse roomResponse = discussionRoomMapper.toResponseWithCategory(room);
            enrichRoomResponse(roomResponse);

            RealTimeMessage notification = RealTimeMessage.builder()
                    .type(actionType)
                    .roomId(room.getId())
                    .room(roomResponse)
                    .actionBy(actionBy)
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/discussion/project/" + room.getProject().getId(), notification);
        } catch (Exception e) {
            log.error("Failed to send room update notification: {}", e.getMessage());
        }
    }

    private void notifyMessageUpdate(Long roomId, DiscussionMessageResponse message, String actionType, String actionBy) {
        try {
            RealTimeMessage notification = RealTimeMessage.builder()
                    .type(actionType)
                    .roomId(roomId)
                    .message(message)
                    .actionBy(actionBy)
                    .timestamp(LocalDateTime.now())
                    .build();

            messagingTemplate.convertAndSend("/topic/discussion/room/" + roomId, notification);
        } catch (Exception e) {
            log.error("Failed to send message update notification: {}", e.getMessage());
        }
    }
}