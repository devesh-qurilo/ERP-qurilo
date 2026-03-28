//package com.erp.chat_service.controller;
//
//import com.erp.chat_service.dto.ChatMessageResponse;
//import com.erp.chat_service.dto.ChatRoomResponse;
//import com.erp.chat_service.dto.SendMessageRequest;
//import com.erp.chat_service.service.ChatService;
//import com.erp.chat_service.util.JwtUtil;
//import jakarta.validation.Valid;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/chat")
//public class ChatController {
//
//    @Autowired
//    private ChatService chatService;
//
//    @Autowired
//    private JwtUtil jwtUtil;
//
//    /**
//     * 1. Send Message (Text or File)
//     * Frontend Use: When user sends a message or file
//     */
//    @PostMapping("/send")
//    public ResponseEntity<ChatMessageResponse> sendMessage(
//            @RequestHeader("Authorization") String authorizationHeader,
//            @Valid @ModelAttribute SendMessageRequest request) {
//
//        String senderId = extractEmployeeIdFromHeader(authorizationHeader);
//        log.info("Sending message from {} to {}", senderId, request.getReceiverId());
//
//        ChatMessageResponse response = chatService.sendMessageAndGetResponse(request, senderId);
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * 2. Get Chat History with Specific User
//     * Frontend Use: When opening chat with someone
//     */
//    @GetMapping("/history/{otherEmployeeId}")
//    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
//            @RequestHeader("Authorization") String authorizationHeader,
//            @PathVariable String otherEmployeeId) {
//
//        String currentEmployeeId = extractEmployeeIdFromHeader(authorizationHeader);
//        log.info("Fetching chat history between {} and {}", currentEmployeeId, otherEmployeeId);
//
//        List<ChatMessageResponse> history = chatService.getChatHistory(currentEmployeeId, otherEmployeeId);
//        return ResponseEntity.ok(history);
//    }
//
//    /**
//     * 3. Get All Chat Rooms (Conversations)
//     * Frontend Use: Show chat list/sidebar
//     */
//    @GetMapping("/rooms")
//    public ResponseEntity<List<ChatRoomResponse>> getChatRooms(
//            @RequestHeader("Authorization") String authorizationHeader) {
//
//        String employeeId = extractEmployeeIdFromHeader(authorizationHeader);
//        log.info("Fetching chat rooms for user {}", employeeId);
//
//        List<ChatRoomResponse> chatRooms = chatService.getChatRooms(employeeId);
//        return ResponseEntity.ok(chatRooms);
//    }
//
//    /**
//     * 4. Mark Messages as Read
//     * Frontend Use: When user opens/reads messages
//     */
//    @PostMapping("/mark-read/{otherEmployeeId}")
//    public ResponseEntity<Void> markMessagesAsRead(
//            @RequestHeader("Authorization") String authorizationHeader,
//            @PathVariable String otherEmployeeId) {
//
//        String currentEmployeeId = extractEmployeeIdFromHeader(authorizationHeader);
//        log.info("Marking messages as read for user {} from {}", currentEmployeeId, otherEmployeeId);
//
//        chatService.markMessagesAsRead(currentEmployeeId, otherEmployeeId);
//        return ResponseEntity.ok().build();
//    }
//
//    /**
//     * 5. Delete Message (Soft Delete - Only for current user)
//     * Frontend Use: When user deletes a message from their view
//     */
//    @DeleteMapping("/message/{messageId}")
//    public ResponseEntity<Void> deleteMessage(
//            @RequestHeader("Authorization") String authorizationHeader,
//            @PathVariable Long messageId) {
//
//        String employeeId = extractEmployeeIdFromHeader(authorizationHeader);
//        log.info("Deleting message {} for user {}", messageId, employeeId);
//
//        chatService.deleteMessageForUser(messageId, employeeId);
//        return ResponseEntity.ok().build();
//    }
//
//    /**
//     * 6. Health Check
//     * Frontend Use: Check if service is running
//     */
//    @GetMapping("/health")
//    public ResponseEntity<String> healthCheck() {
//        return ResponseEntity.ok("Chat Service is running");
//    }
//
//    private String extractEmployeeIdFromHeader(String authorizationHeader) {
//        try {
//            String token = jwtUtil.extractTokenFromHeader(authorizationHeader);
//            return jwtUtil.extractEmployeeId(token);
//        } catch (Exception e) {
//            log.error("Error extracting employeeId: {}", e.getMessage());
//            return null;
//        }
//    }
//}

package com.erp.chat_service.controller;

import com.erp.chat_service.dto.ChatMessageResponse;
import com.erp.chat_service.dto.ChatRoomResponse;
import com.erp.chat_service.dto.SendMessageRequest;
import com.erp.chat_service.service.ChatService;
import com.erp.chat_service.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 1. Send Message (Text or File)
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @ModelAttribute SendMessageRequest request) {

        String senderId = extractEmployeeIdFromHeader(authorizationHeader);
        if (senderId == null) {
            return ResponseEntity.status(401).body("Unauthorized: invalid or missing token");
        }

        log.info("Sending message from {} to {}", senderId, request.getReceiverId());
        ChatMessageResponse response = chatService.sendMessageAndGetResponse(request, senderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. Get Chat History with Specific User
     */
    @GetMapping("/history/{otherEmployeeId}")
    public ResponseEntity<?> getChatHistory(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String otherEmployeeId) {

        String currentEmployeeId = extractEmployeeIdFromHeader(authorizationHeader);
        if (currentEmployeeId == null) {
            return ResponseEntity.status(401).body("Unauthorized: invalid or missing token");
        }

        log.info("Fetching chat history between {} and {}", currentEmployeeId, otherEmployeeId);
        List<ChatMessageResponse> history = chatService.getChatHistory(currentEmployeeId, otherEmployeeId);
        return ResponseEntity.ok(history);
    }

    /**
     * 3. Get All Chat Rooms (Conversations)
     */
    @GetMapping("/rooms")
    public ResponseEntity<?> getChatRooms(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {

        String employeeId = extractEmployeeIdFromHeader(authorizationHeader);
        if (employeeId == null) {
            return ResponseEntity.status(401).body("Unauthorized: invalid or missing token");
        }

        log.info("Fetching chat rooms for user {}", employeeId);
        List<ChatRoomResponse> chatRooms = chatService.getChatRooms(employeeId);
        return ResponseEntity.ok(chatRooms);
    }

    /**
     * 4. Mark Messages as Read
     */
    @PostMapping("/mark-read/{otherEmployeeId}")
    public ResponseEntity<?> markMessagesAsRead(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String otherEmployeeId) {

        String currentEmployeeId = extractEmployeeIdFromHeader(authorizationHeader);
        if (currentEmployeeId == null) {
            return ResponseEntity.status(401).body("Unauthorized: invalid or missing token");
        }

        log.info("Marking messages as read for user {} from {}", currentEmployeeId, otherEmployeeId);
        chatService.markMessagesAsRead(currentEmployeeId, otherEmployeeId);
        return ResponseEntity.ok().build();
    }

    /**
     * 5. Delete Message (Soft Delete - Only for current user)
     */
    @DeleteMapping("/message/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable Long messageId) {

        String employeeId = extractEmployeeIdFromHeader(authorizationHeader);
        if (employeeId == null) {
            return ResponseEntity.status(401).body("Unauthorized: invalid or missing token");
        }

        log.info("Deleting message {} for user {}", messageId, employeeId);
        chatService.deleteMessageForUser(messageId, employeeId);
        return ResponseEntity.ok().build();
    }

    /**
     * 6. Health Check
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Chat Service is running");
    }

    private String extractEmployeeIdFromHeader(String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                log.warn("Authorization header missing or not Bearer");
                return null;
            }
            String token = jwtUtil.extractTokenFromHeader(authorizationHeader);
            return jwtUtil.extractEmployeeId(token);
        } catch (Exception e) {
            log.error("Error extracting employeeId from header: {}", e.getMessage());
            return null;
        }
    }
}
