package com.erp.client_service.service.client.impl;

import com.erp.client_service.dto.client.ClientRequestDto;
import com.erp.client_service.dto.client.ClientResponseDto;
import com.erp.client_service.entity.Client;
import com.erp.client_service.entity.Company;
import com.erp.client_service.event.events.ClientCreatedEvent;

import com.erp.client_service.exception.DuplicateResourceException;
import com.erp.client_service.exception.ResourceNotFoundException;
import com.erp.client_service.repository.ClientRepository;
import com.erp.client_service.repository.CompanyRepository;

import com.erp.client_service.service.client.ClientService;
import com.erp.client_service.service.supabase.SupabaseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final SupabaseService supabaseService;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional
    public ClientResponseDto createClient(ClientRequestDto dto, MultipartFile profilePicture, MultipartFile companyLogo, String addedBy) throws IOException {
        // uniqueness
        clientRepository.findByEmail(dto.getEmail()).ifPresent(c -> { throw new DuplicateResourceException("Email already exists"); });
        clientRepository.findByMobile(dto.getMobile()).ifPresent(c -> { throw new DuplicateResourceException("Mobile already exists"); });

        String profileUrl = null;
        String companyLogoUrl = null;

        if (profilePicture != null && !profilePicture.isEmpty()) {
            profileUrl = supabaseService.uploadFile(profilePicture, "clients/profile", true); // restrict jpg only
        }
        if (companyLogo != null && !companyLogo.isEmpty()) {
            companyLogoUrl = supabaseService.uploadFile(companyLogo, "clients/company", false);
        }

        Client client = Client.builder()
                .clientId(generateClientId())
                .name(dto.getName())
                .email(dto.getEmail())
                .mobile(dto.getMobile())
                .country(dto.getCountry())
                .gender(dto.getGender())
                .category(dto.getCategory())
                .subCategory(dto.getSubCategory())
                .language(dto.getLanguage())
                .receiveEmail(dto.getReceiveEmail() == null ? true : dto.getReceiveEmail())
                .profilePictureUrl(profileUrl)
                .companyLogoUrl(companyLogoUrl)
                .status("ACTIVE")
                .skype(dto.getSkype())
                .linkedIn(dto.getLinkedIn())
                .twitter(dto.getTwitter())
                .facebook(dto.getFacebook())
                .addedBy(addedBy)
                .createdAt(Instant.now())
                .build();

        if (dto.getCompany() != null) {
            Company company = Company.builder()
                    .companyName(dto.getCompany().getCompanyName())
                    .website(dto.getCompany().getWebsite())
                    .officePhone(dto.getCompany().getOfficePhone())
                    .taxName(dto.getCompany().getTaxName())
                    .gstVatNo(dto.getCompany().getGstVatNo())
                    .address(dto.getCompany().getAddress())
                    .city(dto.getCompany().getCity())
                    .state(dto.getCompany().getState())
                    .postalCode(dto.getCompany().getPostalCode())
                    .shippingAddress(dto.getCompany().getShippingAddress())
                    .companyLogoUrl(companyLogoUrl)
                    .client(client)
                    .build();
            client.setCompany(company);
        }

        Client saved = clientRepository.save(client);

        // publish event for welcome mail/notification
        if (saved.getEmail() != null && saved.getReceiveEmail()) {
            publisher.publishEvent(new ClientCreatedEvent(this, saved.getId(), saved.getClientId(), saved.getEmail(), saved.getAddedBy()));
        }

        return toDto(saved);
    }

//    private String generateClientId() {
//        long next = clientRepository.count() + 1;
//        return String.format("CLI%03d", next);
//    }

//    @PersistenceContext
//    private EntityManager em;
//
//    private String generateClientId() {
//        Long seq = ((Number) em.createNativeQuery("SELECT nextval('client_id_seq')").getSingleResult()).longValue();
//        return String.format("CLI%03d", seq);
//    }

    private String generateClientId() {

        // Step 1: last client fetch + extract number
        Client last = clientRepository.findTopByOrderByIdDesc().orElse(null);
        int nextNum = 1;

        if (last != null && last.getClientId() != null) {
            String numeric = last.getClientId().replaceAll("\\D", "");
            try {
                nextNum = Integer.parseInt(numeric) + 1;
            } catch (NumberFormatException ignore) {
                nextNum = 1;
            }
        }

        // Step 2: loop until we find NON-existing clientId
        String id;
        do {
            id = String.format("CLI%03d", nextNum);  // format CLI001 etc
            nextNum++;
        } while (clientRepository.existsByClientId(id));

        return id;
    }




    @Override
    public ClientResponseDto getClient(Long id) {
        Client c = clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        return toDto(c);
    }

    @Override
    public List<ClientResponseDto> listClients(String search, String status) {
        // simple listing; you can add filtering/paging later
        return clientRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientResponseDto updateClient(Long id, ClientRequestDto dto, MultipartFile profilePicture, MultipartFile companyLogo, String updatedBy) throws IOException {
        Client c = clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // uniqueness checks only if changed
        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(c.getEmail())) {
            clientRepository.findByEmail(dto.getEmail()).ifPresent(x -> { throw new DuplicateResourceException("Email already exists"); });
            c.setEmail(dto.getEmail());
        }
        if (dto.getMobile() != null && !dto.getMobile().equalsIgnoreCase(c.getMobile())) {
            clientRepository.findByMobile(dto.getMobile()).ifPresent(x -> { throw new DuplicateResourceException("Mobile already exists"); });
            c.setMobile(dto.getMobile());
        }

        if (dto.getName() != null) c.setName(dto.getName());
        if (dto.getCountry() != null) c.setCountry(dto.getCountry());
        if (dto.getGender() != null) c.setGender(dto.getGender());
        if (dto.getCategory() != null) c.setCategory(dto.getCategory());
        if (dto.getSubCategory() != null) c.setSubCategory(dto.getSubCategory());
        c.setUpdatedAt(Instant.now());

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String url = supabaseService.uploadFile(profilePicture, "clients/profile", true);
            c.setProfilePictureUrl(url);
        }
        if (companyLogo != null && !companyLogo.isEmpty()) {
            String url = supabaseService.uploadFile(companyLogo, "clients/company", false);
            c.setCompanyLogoUrl(url);
            if (c.getCompany() != null) c.getCompany().setCompanyLogoUrl(url);
        }

        if (dto.getCompany() != null) {
            Company comp = c.getCompany();
            if (comp == null) {
                comp = new Company();
                comp.setClient(c);
                c.setCompany(comp);
            }
            if (dto.getCompany().getCompanyName() != null) comp.setCompanyName(dto.getCompany().getCompanyName());
            if (dto.getCompany().getWebsite() != null) comp.setWebsite(dto.getCompany().getWebsite());
            if (dto.getCompany().getOfficePhone() != null) comp.setOfficePhone(dto.getCompany().getOfficePhone());
            if (dto.getCompany().getTaxName() != null) comp.setTaxName(dto.getCompany().getTaxName());
            if (dto.getCompany().getGstVatNo() != null) comp.setGstVatNo(dto.getCompany().getGstVatNo());
            if (dto.getCompany().getAddress() != null) comp.setAddress(dto.getCompany().getAddress());
            if (dto.getCompany().getCity() != null) comp.setCity(dto.getCompany().getCity());
            if (dto.getCompany().getState() != null) comp.setState(dto.getCompany().getState());
            if (dto.getCompany().getPostalCode() != null) comp.setPostalCode(dto.getCompany().getPostalCode());
            if (dto.getCompany().getShippingAddress() != null) comp.setShippingAddress(dto.getCompany().getShippingAddress());
        }

        Client saved = clientRepository.save(c);
        return toDto(saved);
    }

    @Override
    public void deleteClient(Long id, String deletedBy) {
        // could add soft-delete; for now hard delete
        if (!clientRepository.existsById(id)) throw new ResourceNotFoundException("Client not found");
        clientRepository.deleteById(id);
    }

    private ClientResponseDto toDto(Client c) {
        com.erp.client_service.dto.client.CompanyDto compDto = null;
        if (c.getCompany() != null) {
            Company comp = c.getCompany();
            compDto = com.erp.client_service.dto.client.CompanyDto.builder()
                    .companyName(comp.getCompanyName())
                    .website(comp.getWebsite())
                    .officePhone(comp.getOfficePhone())
                    .taxName(comp.getTaxName())
                    .gstVatNo(comp.getGstVatNo())
                    .address(comp.getAddress())
                    .city(comp.getCity())
                    .state(comp.getState())
                    .postalCode(comp.getPostalCode())
                    .shippingAddress(comp.getShippingAddress())
                    .companyLogoUrl(comp.getCompanyLogoUrl())
                    .build();
        }
        return com.erp.client_service.dto.client.ClientResponseDto.builder()
                .id(c.getId())
                .clientId(c.getClientId())
                .name(c.getName())
                .email(c.getEmail())
                .mobile(c.getMobile())
                .country(c.getCountry())
                .gender(c.getGender())
                .category(c.getCategory())
                .subCategory(c.getSubCategory())
                .profilePictureUrl(c.getProfilePictureUrl())
                .language(c.getLanguage())
                .receiveEmail(c.getReceiveEmail())
                .status(c.getStatus())
                .skype(c.getSkype())
                .linkedIn(c.getLinkedIn())
                .twitter(c.getTwitter())
                .facebook(c.getFacebook())
                .company(compDto)
                .companyLogoUrl(c.getCompanyLogoUrl())
                .addedBy(c.getAddedBy())
                .createdAt(c.getCreatedAt() == null ? null : c.getCreatedAt().toString())
                .build();
    }
}
