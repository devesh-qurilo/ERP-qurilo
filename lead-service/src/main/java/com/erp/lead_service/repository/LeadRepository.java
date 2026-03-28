package com.erp.lead_service.repository;

import com.erp.lead_service.entity.Lead;
import com.erp.lead_service.entity.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    Optional<Lead> findByEmail(String email);
    Optional<Lead> findByMobileNumber(String mobileNumber);

    List<Lead> findByLeadOwner(String leadOwner);
    List<Lead> findByAddedBy(String addedBy);

    @Query("SELECT l FROM Lead l WHERE l.leadOwner = :employeeId OR l.addedBy = :employeeId")
    List<Lead> findByOwnerOrCreator(@Param("employeeId") String employeeId);

    List<Lead> findByStatus(LeadStatus status);

    @Query("SELECT l FROM Lead l WHERE " +
            "LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(l.companyName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Lead> searchLeads(@Param("search") String search);

    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);
    List<Lead> findByLeadOwnerOrAddedBy(String owner, String addedBy);


    // existing methods...
    Optional<Lead> findByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lead l WHERE LOWER(l.email) = LOWER(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    Optional<Lead> findByNameIgnoreCase(String name);

}