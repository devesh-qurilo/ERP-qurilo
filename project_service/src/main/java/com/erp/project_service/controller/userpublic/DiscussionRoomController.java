package com.erp.project_service.controller.userpublic;

import com.erp.project_service.dto.Discussion.DiscussionRoomRequest;
import com.erp.project_service.dto.Discussion.DiscussionRoomResponse;
import com.erp.project_service.service.interfaces.DiscussionRoomService;
import com.erp.project_service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/discussion-rooms")
@RequiredArgsConstructor
public class DiscussionRoomController {

    private final DiscussionRoomService discussionRoomService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionRoomResponse> createRoom(
            @PathVariable Long projectId,
            @Valid @ModelAttribute DiscussionRoomRequest request) {

        String userId = SecurityUtils.getCurrentUserId();
        DiscussionRoomResponse response = discussionRoomService.createRoom(projectId, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<List<DiscussionRoomResponse>> getProjectRooms(
            @PathVariable Long projectId) {

        String userId = SecurityUtils.getCurrentUserId();
        List<DiscussionRoomResponse> rooms = discussionRoomService.getRoomsByProject(projectId, userId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{roomId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_EMPLOYEE')")
    public ResponseEntity<DiscussionRoomResponse> getRoom(
            @PathVariable Long projectId,
            @PathVariable Long roomId) {

        String userId = SecurityUtils.getCurrentUserId();
        DiscussionRoomResponse room = discussionRoomService.getRoomById(roomId, userId);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long projectId,
            @PathVariable Long roomId) {

        String userId = SecurityUtils.getCurrentUserId();
        discussionRoomService.deleteRoom(roomId, userId);
        return ResponseEntity.noContent().build();
    }
}