package com.erp.project_service.repository;

import com.erp.project_service.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {
    List<ProjectMilestone> findByProjectId(Long projectId);

    @Query("select coalesce(sum(m.milestoneCost),0) from ProjectMilestone m where m.projectId = :projectId")
    BigDecimal sumCostByProjectId(@Param("projectId") Long projectId);

}
