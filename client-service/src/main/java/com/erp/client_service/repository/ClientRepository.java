package com.erp.client_service.repository;

import com.erp.client_service.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByClientId(String clientId);
    Optional<Client> findByEmail(String email);
    Optional<Client> findByMobile(String mobile);

    boolean existsByClientId(String clientId);
    Optional<Client> findTopByOrderByIdDesc();
}
