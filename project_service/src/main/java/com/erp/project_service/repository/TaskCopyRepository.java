package com.erp.project_service.repository;

import com.erp.project_service.entity.TaskCopy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface TaskCopyRepository extends JpaRepository<TaskCopy, UUID> {}
