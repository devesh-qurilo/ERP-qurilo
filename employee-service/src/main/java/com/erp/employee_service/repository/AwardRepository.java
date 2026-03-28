package com.erp.employee_service.repository;

import com.erp.employee_service.entity.award.Award;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AwardRepository extends JpaRepository<Award, Long> {
    List<Award> findByIsActiveTrueOrderByCreatedAtDesc();
    List<Award> findAllByOrderByCreatedAtDesc();
}