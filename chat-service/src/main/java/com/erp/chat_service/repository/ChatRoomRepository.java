package com.erp.chat_service.repository;

import com.erp.chat_service.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    Optional<ChatRoom> findByParticipant1IdAndParticipant2Id(String participant1Id, String participant2Id);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.participant1Id = :employeeId OR cr.participant2Id = :employeeId")
    List<ChatRoom> findAllByEmployeeId(@Param("employeeId") String employeeId);

    @Query("SELECT cr FROM ChatRoom cr WHERE (cr.participant1Id = :emp1 AND cr.participant2Id = :emp2) OR (cr.participant1Id = :emp2 AND cr.participant2Id = :emp1)")
    Optional<ChatRoom> findChatRoomBetweenEmployees(@Param("emp1") String emp1, @Param("emp2") String emp2);
}