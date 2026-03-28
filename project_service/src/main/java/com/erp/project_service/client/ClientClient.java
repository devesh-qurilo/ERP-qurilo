package com.erp.project_service.client;

import com.erp.project_service.dto.common.ClientMetaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "client-service", path = "/clients")
public interface ClientClient {

    @GetMapping("/{id}")
    ClientMetaDto getClient(@PathVariable("id") Long id);

    @GetMapping("/client/{clientId}")  // ✅ Fixed path variable name
    ClientMetaDto getClients(@PathVariable("clientId") String clientId); // ✅ Correct parameter name

    @GetMapping("/internal/exists")
    Boolean existsByEmail(@RequestParam("email") String email); // ✅ Added @RequestParam
}