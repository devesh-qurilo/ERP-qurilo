package com.erp.finance_servic.client;

import com.erp.finance_servic.dto.invoice.response.ClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "client-service", path = "/clients")
public interface ClientServiceClient {
//
//    @GetMapping("/client/{clientId}")
//    ClientResponse getClientById(
//            @PathVariable String clientId);
    @GetMapping("/internal/client/{clientId}")
    ClientResponse getClientByIdInternal(@PathVariable String clientId,
                                     @RequestHeader("X-Internal-Api-Key") String internalApiKey);


    /**
     * Search/list endpoint. We call this as fallback to obtain full client DTOs.
     * Example: GET /clients?search=INV-123
     */
    @GetMapping
    List<ClientResponse> searchClients(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader("X-Internal-Api-Key") String internalApiKey
    );
}
