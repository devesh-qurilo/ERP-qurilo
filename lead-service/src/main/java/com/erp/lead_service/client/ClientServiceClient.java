package com.erp.lead_service.client;

import com.erp.lead_service.dto.client.ClientResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(name = "client-service", url = "${client-service.url}")
public interface ClientServiceClient {

    @GetMapping("/clients/email/{email}")
    List<ClientResponseDto> getClientsByEmail(@PathVariable("email") String email,
                                              @RequestHeader("Authorization") String authorization);

    // Match the client-service controller which expects multipart/form-data with a "client" part (JSON string)
    @PostMapping(value = "/clients", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ClientResponseDto createClient(
            @RequestPart("client") String clientJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "companyLogo", required = false) MultipartFile companyLogo,
            @RequestHeader("Authorization") String authorization
    );
}
