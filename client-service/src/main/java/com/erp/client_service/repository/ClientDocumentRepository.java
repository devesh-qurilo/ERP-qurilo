package com.erp.client_service.repository;

import com.erp.client_service.entity.ClientDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientDocumentRepository extends JpaRepository<ClientDocument, Long> {
    List<ClientDocument> findByClientId(Long clientId);
}
