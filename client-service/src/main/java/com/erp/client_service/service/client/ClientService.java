package com.erp.client_service.service.client;

import com.erp.client_service.dto.client.ClientRequestDto;
import com.erp.client_service.dto.client.ClientResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ClientService {
    ClientResponseDto createClient(ClientRequestDto dto, MultipartFile profilePicture, MultipartFile companyLogo, String addedBy) throws IOException;
    ClientResponseDto getClient(Long id);
    List<ClientResponseDto> listClients(String search, String status);
    ClientResponseDto updateClient(Long id, ClientRequestDto dto, MultipartFile profilePicture, MultipartFile companyLogo, String updatedBy) throws IOException;
    void deleteClient(Long id, String deletedBy);
}
