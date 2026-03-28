package com.erp.project_service.controller.userpublic;

import com.erp.project_service.dto.Discussion.DiscussionMessageRequest;
import com.erp.project_service.dto.Discussion.DiscussionMessageResponse;
import com.erp.project_service.service.interfaces.DiscussionRoomService;
import com.erp.project_service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects/discussion-rooms/{roomId}/messages")
@RequiredArgsConstructor
public class DiscussionMessageController {

    private final DiscussionRoomService discussionRoomService;

//    @PostMapping
//    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
//    public ResponseEntity<DiscussionMessageResponse> sendMessage(
//            @PathVariable Long roomId,
//            @RequestParam MultipartFile file,
//            @RequestBody DiscussionMessageRequest request) {
//
//        String userId = SecurityUtils.getCurrentUserId();
//        DiscussionMessageResponse response = discussionRoomService.sendMessage(roomId, request, userId);
//        return ResponseEntity.ok(response);
//    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionMessageResponse> sendMessage(
            @PathVariable Long roomId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("content") String content,
            @RequestParam(value = "parentMessageId", required = false) Long parentMessageId) {

        String userId = SecurityUtils.getCurrentUserId();

        // DiscussionMessageRequest object manually banao
        DiscussionMessageRequest request = DiscussionMessageRequest.builder()
                .content(content)
                .parentMessageId(parentMessageId)
                .file(file)
                .build();

        DiscussionMessageResponse response = discussionRoomService.sendMessage(roomId, request,userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/file")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionMessageResponse> sendFile(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file) {

        String userId = SecurityUtils.getCurrentUserId();
        DiscussionMessageResponse response = discussionRoomService.sendFileMessage(roomId, file, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<List<DiscussionMessageResponse>> getRoomMessages(
            @PathVariable Long roomId) {

        String userId = SecurityUtils.getCurrentUserId();
        List<DiscussionMessageResponse> messages = discussionRoomService.getRoomMessages(roomId, userId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{messageId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionMessageResponse> getMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId) {

        String userId = SecurityUtils.getCurrentUserId();
        DiscussionMessageResponse message = discussionRoomService.getMessage(messageId, userId);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/{messageId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionMessageResponse> updateMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestBody String content) {

        String userId = SecurityUtils.getCurrentUserId();
        DiscussionMessageResponse response = discussionRoomService.updateMessage(messageId, content, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long roomId,
            @PathVariable Long messageId) {

        String userId = SecurityUtils.getCurrentUserId();
        discussionRoomService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{parentMessageId}/replies")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionMessageResponse> replyToMessage(
            @PathVariable Long roomId,
            @PathVariable Long parentMessageId,
            @RequestBody DiscussionMessageRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        DiscussionMessageResponse response = discussionRoomService.replyToMessage(parentMessageId, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{parentMessageId}/replies")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<List<DiscussionMessageResponse>> getMessageReplies(
            @PathVariable Long roomId,
            @PathVariable Long parentMessageId) {

        String userId = SecurityUtils.getCurrentUserId();
        List<DiscussionMessageResponse> replies = discussionRoomService.getMessageReplies(parentMessageId, userId);
        return ResponseEntity.ok(replies);
    }

    @PostMapping("/{messageId}/mark-best-reply")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionMessageResponse> markAsBestReply(
            @PathVariable Long roomId,
            @PathVariable Long messageId) {

        String userId = SecurityUtils.getCurrentUserId();
        DiscussionMessageResponse response = discussionRoomService.markAsBestReply(messageId, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{messageId}/unmark-best-reply")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionMessageResponse> unmarkBestReply(
            @PathVariable Long roomId,
            @PathVariable Long messageId) {

        String userId = SecurityUtils.getCurrentUserId();
        DiscussionMessageResponse response = discussionRoomService.unmarkBestReply(messageId, userId);
        return ResponseEntity.ok(response);
    }
}