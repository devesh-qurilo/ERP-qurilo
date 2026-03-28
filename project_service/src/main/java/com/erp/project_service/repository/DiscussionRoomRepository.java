package com.erp.project_service.repository;

import com.erp.project_service.entity.DiscussionRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscussionRoomRepository extends JpaRepository<DiscussionRoom, Long> {

    List<DiscussionRoom> findByProjectIdAndIsActiveTrue(Long projectId);

    Optional<DiscussionRoom> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT COUNT(dm) FROM DiscussionMessage dm WHERE dm.room.id = :roomId AND dm.isDeleted = false")
    Long countMessagesByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT dr FROM DiscussionRoom dr WHERE dr.project.id = :projectId AND dr.isActive = true ORDER BY dr.updatedAt DESC")
    List<DiscussionRoom> findActiveRoomsByProjectId(@Param("projectId") Long projectId);

    boolean existsByIdAndProjectAssignedEmployeeIdsContains(Long roomId, String employeeId);

    @Query("SELECT dr FROM DiscussionRoom dr JOIN dr.project p WHERE p.id = :projectId AND (:employeeId MEMBER OF p.assignedEmployeeIds OR :isAdmin = true) AND dr.isActive = true")
    List<DiscussionRoom> findAccessibleRoomsByProject(@Param("projectId") Long projectId,
                                                      @Param("employeeId") String employeeId,
                                                      @Param("isAdmin") boolean isAdmin);
}