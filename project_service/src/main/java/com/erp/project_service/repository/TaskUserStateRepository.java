// repository/TaskUserStateRepository.java
package com.erp.project_service.repository;

import com.erp.project_service.entity.TaskUserState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface TaskUserStateRepository extends JpaRepository<TaskUserState, Long> {
    Optional<TaskUserState> findByTaskIdAndUserId(Long taskId, String userId);
    List<TaskUserState> findByUserIdAndPinnedAtNotNullOrderByPinnedAtDesc(String userId);

    // bulk for list enrichment
    List<TaskUserState> findByUserIdAndTaskIdIn(String userId, Collection<Long> taskIds);
}
