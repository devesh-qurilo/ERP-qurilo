package com.erp.chat_service.repository;

import com.erp.chat_service.dto.ChatMessageResponse;
import com.erp.chat_service.entity.ChatMessage;
import com.erp.chat_service.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(String chatRoomId);

    List<ChatMessage> findByChatRoomId(String chatRoomId);

    List<ChatMessage> findByReceiverIdAndStatus(String receiverId, MessageStatus status);
    Optional<ChatMessage> findFirstByChatRoomIdOrderByCreatedAtDesc(String chatRoomId);


    @Modifying
    @Query("UPDATE ChatMessage cm SET cm.status = :status WHERE cm.id IN :messageIds")
    void updateMessageStatus(@Param("messageIds") List<Long> messageIds, @Param("status") MessageStatus status);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.receiverId = :receiverId AND cm.status = 'SENT' AND :receiverId NOT MEMBER OF cm.deletedForUsers")
    Long countUnreadMessages(@Param("receiverId") String receiverId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm WHERE cm.chatRoomId = :chatRoomId AND cm.receiverId = :receiverId AND cm.status = 'SENT' AND :receiverId NOT MEMBER OF cm.deletedForUsers")
    Long countUnreadMessagesInChatRoom(@Param("chatRoomId") String chatRoomId, @Param("receiverId") String receiverId);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.createdAt < :beforeDate")
    List<ChatMessage> findMessagesOlderThan(@Param("beforeDate") LocalDateTime beforeDate);

    // Add this method to eagerly fetch file attachments
    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.fileAttachment WHERE m.chatRoomId = :chatRoomId ORDER BY m.createdAt ASC")
    List<ChatMessage> findByChatRoomIdWithFileAttachment(@Param("chatRoomId") String chatRoomId);

    // Update existing method
    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.fileAttachment WHERE m.chatRoomId = :chatRoomId AND :userId NOT MEMBER OF m.deletedForUsers ORDER BY m.createdAt ASC")
    List<ChatMessage> findByChatRoomIdAndNotDeletedForUser(@Param("chatRoomId") String chatRoomId, @Param("userId") String userId);

    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.fileAttachment WHERE m.chatRoomId = :chatRoomId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<ChatMessage> findLastMessageByChatRoomId(@Param("chatRoomId") String chatRoomId);

// REPLACE THE PROBLEMATIC METHOD WITH THESE:

    // Search messages where current user is either sender or receiver
    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(m.senderId = :userId OR m.receiverId = :userId)")
    List<ChatMessage> searchMessagesForUser(@Param("query") String query, @Param("userId") String userId);

    // Search messages in specific chat room
    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(m.chatRoomId = :chatRoomId)")
    List<ChatMessage> searchMessagesInChatRoom(@Param("query") String query, @Param("chatRoomId") String chatRoomId);

    // Simple content search (without user filter)
    List<ChatMessage> findByContentContainingIgnoreCase(String content);}