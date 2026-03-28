package com.erp.client_service.controller;

import com.erp.client_service.dto.Import.ImportResult;
import com.erp.client_service.dto.client.ClientRequestDto;
import com.erp.client_service.dto.client.ClientResponseDto;

import com.erp.client_service.entity.Client;
import com.erp.client_service.repository.ClientRepository;
import com.erp.client_service.service.Import.ClientCsvImportService;
import com.erp.client_service.service.client.ClientService;
import com.erp.client_service.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ClientRepository clientRepository;
    private final ClientCsvImportService clientCsvImportService;
    // imports: @Value etc.
    @Value("${internal.api.key}")
    private String expectedInternalApiKey;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ClientResponseDto> createClient(
            @RequestPart("client") @Valid String clientJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "companyLogo", required = false) MultipartFile companyLogo,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth
    ) throws IOException {
        // parse JSON -> DTO and validate manually
        ClientRequestDto dto1 = objectMapper.readValue(clientJson, ClientRequestDto.class);
        String addedBy = jwtUtil.extractSubject(auth.substring(7));
        ClientResponseDto response = clientService.createClient(dto1, profilePicture, companyLogo, addedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<ClientResponseDto> listClients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return clientService.listClients(search, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ClientResponseDto getClient(@PathVariable Long id) {
        return clientService.getClient(id);
    }


    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ClientResponseDto getClientById(@PathVariable String clientId) {
        Client client = clientRepository.findByClientId(clientId).orElseThrow(() -> new RuntimeException("Not found"));

        // Convert Client entity to ClientMetaDto
        return ClientResponseDto.builder()
                .clientId(client.getClientId())
                .name(client.getName())
                .profilePictureUrl(client.getProfilePictureUrl()) // Adjust field name as per your entity
                .build();
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ClientResponseDto updateClient(
            @PathVariable Long id,
            @RequestPart("client") @Valid String clientJson,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture,
            @RequestPart(value = "companyLogo", required = false) MultipartFile companyLogo,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth
    ) throws IOException {
        ClientRequestDto dto = objectMapper.readValue(clientJson, ClientRequestDto.class);
        String updatedBy = jwtUtil.extractSubject(auth.substring(7));
        return clientService.updateClient(id, dto, profilePicture, companyLogo, updatedBy);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> deleteClient(
            @PathVariable Long id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth
    ) {
        String deletedBy = jwtUtil.extractSubject(auth.substring(7));
        clientService.deleteClient(id, deletedBy);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/exists")
    public Boolean checkClientExists(
            @RequestParam("email") String email,
            @RequestHeader("X-Internal-Api-Key") String internalApiKey) {

        // Simple internal security check
        if (!"your-internal-api-key".equals(internalApiKey)) {
            throw new RuntimeException("Unauthorized internal access");
        }

        return clientRepository.findByEmail(email).isPresent();
    }

    @GetMapping("/internal/client/{clientId}")
    public ResponseEntity<ClientResponseDto> getClientByIdInternal(
            @PathVariable String clientId,
            @RequestHeader("X-Internal-Api-Key") String internalApiKey
    ) {
        if (internalApiKey == null || !internalApiKey.equals(expectedInternalApiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        ClientResponseDto dto = ClientResponseDto.builder()
                .clientId(client.getClientId())
                .name(client.getName())
                .profilePictureUrl(client.getProfilePictureUrl()) //
                // add all other fields you need in full DTO
                .build();

        return ResponseEntity.ok(dto);
    }

    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<com.erp.client_service.dto.Import.ImportResult>> importClients(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        List<ImportResult> res = clientCsvImportService
                .importClientsFromCsv(file, auth);
        return ResponseEntity.ok(res);
    }

}
