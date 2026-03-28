package com.erp.client_service.repository;

import com.erp.client_service.entity.ClientNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientNoteRepository extends JpaRepository<ClientNote, Long> {
    List<ClientNote> findByClientId(Long clientId);
}
