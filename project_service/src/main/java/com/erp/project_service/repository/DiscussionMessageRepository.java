package com.erp.project_service.repository;

import com.erp.project_service.entity.DiscussionMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscussionMessageRepository extends JpaRepository<DiscussionMessage, Long> {

    List<DiscussionMessage> findByRoomIdAndParentMessageIsNullAndIsDeletedFalseOrderByCreatedAtDesc(Long roomId);

    List<DiscussionMessage> findByParentMessageIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentMessageId);

    Optional<DiscussionMessage> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT dm FROM DiscussionMessage dm WHERE dm.room.id = :roomId AND dm.isDeleted = false ORDER BY dm.createdAt DESC")
    List<DiscussionMessage> findLatestMessagesByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT dm FROM DiscussionMessage dm WHERE dm.id = :messageId AND (dm.senderId = :userId OR :isAdmin = true)")
    Optional<DiscussionMessage> findByIdAndSenderOrAdmin(@Param("messageId") Long messageId,
                                                         @Param("userId") String userId,
                                                         @Param("isAdmin") boolean isAdmin);

    @Query("SELECT dm FROM DiscussionMessage dm WHERE dm.room.id = :roomId AND dm.isDeleted = false ORDER BY dm.createdAt ASC")
    List<DiscussionMessage> findCompleteThreadByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(dm) FROM DiscussionMessage dm WHERE dm.room.id = :roomId AND dm.parentMessage.id = :parentId AND dm.isDeleted = false")
    Long countRepliesByParentId(@Param("parentId") Long parentId, @Param("roomId") Long roomId);

    Long countMessagesByRoomId(Long id);

    @Query("SELECT dm FROM DiscussionMessage dm WHERE dm.room.id = :roomId AND dm.isBestReply = true AND dm.isDeleted = false")
    Optional<DiscussionMessage> findBestReplyByRoomId(@Param("roomId") Long roomId);
}