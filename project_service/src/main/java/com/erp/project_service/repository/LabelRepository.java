package com.erp.project_service.repository;

import com.erp.project_service.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findByProjectId(Long projectId);

}
