package com.erp.employee_service.repository;


import com.erp.employee_service.entity.award.AwardIcon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AwardIconRepository extends JpaRepository<AwardIcon, Long> {
}