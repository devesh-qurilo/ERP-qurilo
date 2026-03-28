////package com.erp.lead_service.service;
////
////import com.erp.lead_service.client.ClientServiceClient;
////import com.erp.lead_service.client.EmployeeServiceClient;
////import com.erp.lead_service.client.NotificationClient;
////import com.erp.lead_service.dto.EmployeeMetaDto;
////import com.erp.lead_service.dto.client.ClientRequestDto;
////import com.erp.lead_service.dto.client.ClientResponseDto;
////import com.erp.lead_service.dto.deal.DealRequestDto;
////import com.erp.lead_service.dto.employee.EmployeeDto;
////import com.erp.lead_service.dto.lead.DealMiniDto;
////import com.erp.lead_service.dto.lead.LeadRequestDto;
////import com.erp.lead_service.dto.lead.LeadResponseDto;
////import com.erp.lead_service.entity.Lead;
////import com.erp.lead_service.entity.LeadStatus;
////import com.erp.lead_service.exception.DuplicateResourceException;
////import com.erp.lead_service.exception.ResourceNotFoundException;
////import com.erp.lead_service.exception.UnauthorizedAccessException;
////import com.erp.lead_service.mapper.DealMapper;
////import com.erp.lead_service.mapper.LeadMapper;
////import com.erp.lead_service.repository.DealRepository;
////import com.erp.lead_service.repository.LeadRepository;
////import com.erp.lead_service.util.JwtUtil;
////import com.fasterxml.jackson.databind.ObjectMapper;
////import feign.FeignException;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.context.ApplicationEventPublisher;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////import java.util.List;
////
////import static com.erp.lead_service.entity.LeadStatus.CONVERTED;
////
////@Service
////@RequiredArgsConstructor
////@Slf4j
////public class LeadServiceImpl implements LeadService {
////
////    private final LeadRepository leadRepository;
////    private final LeadMapper leadMapper;
////    private final EmployeeServiceClient employeeClient;
////    private final JwtUtil jwtUtil;
////    private final DealService dealService;
////    private final DealMapper dealMapper;
////    private final ClientServiceClient clientClient;
////    private final NotificationClient notificationClient;
////    private final DealRepository dealRepository;
////    private final ApplicationEventPublisher eventPublisher;
////    private final ObjectMapper objectMapper;
////
////    @Override
////    @Transactional
////    public LeadResponseDto createLead(LeadRequestDto dto, String authHeader) {
////        // validations for uniqueness
////        if (dto.getEmail() != null && leadRepository.existsByEmail(dto.getEmail())) {
////            throw new DuplicateResourceException("Lead with this email already exists");
////        }
////        if (dto.getMobileNumber() != null && leadRepository.existsByMobileNumber(dto.getMobileNumber())) {
////            throw new DuplicateResourceException("Lead with this mobile number already exists");
////        }
////
////        String token = extractToken(authHeader);
////        String currentEmployeeId = jwtUtil.extractSubject(token);
////        boolean isAdmin = jwtUtil.isAdmin(token);
////
////        // enforce employee assignment rules
////        if (!isAdmin) {
////            dto.setLeadOwner(currentEmployeeId);
////            dto.setAddedBy(currentEmployeeId);
////        } else {
////            if (dto.getLeadOwner() != null) validateEmployeeExists(dto.getLeadOwner(), authHeader);
////            if (dto.getAddedBy() != null) validateEmployeeExists(dto.getAddedBy(), authHeader);
////        }
////
////        Lead lead = leadMapper.toEntity(dto);
////        lead.setAddedBy(dto.getAddedBy() != null ? dto.getAddedBy() : currentEmployeeId);
////        lead.setLeadOwner(dto.getLeadOwner() != null ? dto.getLeadOwner() : currentEmployeeId);
////
////        // save lead (commit will allow listeners to run and create deal)
////        Lead saved = leadRepository.save(lead);
////        log.info("Lead created with id {}", saved.getId());
////
////        // decide whether to attempt auto-convert later (after commit)
////        boolean shouldAttemptAutoConvert = Boolean.TRUE.equals(saved.getAutoConvertToClient());
////
////        // If client explicitly requested to create deal, publish an AFTER_COMMIT event to create it.
////        if (dto.getCreateDeal() != null && dto.getCreateDeal() && dto.getDeal() != null) {
////            try {
////                DealMiniDto d = dto.getDeal();
////                DealRequestDto dealDto = new DealRequestDto();
////                dealDto.setTitle(d.getTitle());
////                dealDto.setPipeline(d.getPipeline());
////                dealDto.setDealStage(d.getDealStage());
////                dealDto.setDealCategory(d.getDealCategory());
////                dealDto.setValue(d.getValue());
////                dealDto.setExpectedCloseDate(d.getExpectedCloseDate());
////                dealDto.setDealAgent(d.getDealAgent());
////                dealDto.setDealWatchers(d.getDealWatchers());
////                dealDto.setDealContact(saved.getName());
////                // Do not set leadId here; listener will set leadId after lead commit.
////
////                // Publish event to create deal after commit (LeadCreateDealListener handles it)
////                eventPublisher.publishEvent(new com.erp.lead_service.event.LeadCreateDealEvent(saved.getId(), dealDto, authHeader));
////                log.info("Published LeadCreateDealEvent for lead {}", saved.getId());
////            } catch (Exception ex) {
////                log.error("Failed to publish LeadCreateDealEvent for lead {}: {}", saved.getId(), ex.getMessage(), ex);
////            }
////        }
////
////        // publish event for auto-conversion AFTER commit (listener will run in AFTER_COMMIT phase)
////        if (shouldAttemptAutoConvert) {
////            try {
////                eventPublisher.publishEvent(new com.erp.lead_service.event.LeadAutoConvertEvent(saved.getId(), authHeader));
////                log.info("Published LeadAutoConvertEvent for lead {}", saved.getId());
////            } catch (Exception e) {
////                log.warn("Failed to publish LeadAutoConvertEvent for lead {}: {}", saved.getId(), e.getMessage(), e);
////            }
////        }
////
////        return leadMapper.toDto(saved);
////    }
////
////
////    @Override
////    public LeadResponseDto getLeadById(Long id, String authHeader) {
////        Lead lead = leadRepository.findById(id)
////                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
////        checkLeadAccess(lead, authHeader);
////
////        LeadResponseDto out = leadMapper.toDto(lead);
////        out.setLeadOwnerMeta(buildEmployeeMeta(out.getLeadOwner(), authHeader));
////        out.setAddedByMeta(buildEmployeeMeta(out.getAddedBy(), authHeader));
////        return out;
////    }
////
////    private void checkLeadAccess(Lead lead, String authHeader) {
////        String token = extractToken(authHeader);
////        String currentEmployeeId = jwtUtil.extractSubject(token);
////        boolean isAdmin = jwtUtil.isAdmin(token);
////
////        if (isAdmin) return;
////
////        if (lead == null) {
////            throw new ResourceNotFoundException("Lead not found");
////        }
////
////        String owner = lead.getLeadOwner();
////        String addedBy = lead.getAddedBy();
////
////        if ((owner != null && owner.equals(currentEmployeeId)) ||
////                (addedBy != null && addedBy.equals(currentEmployeeId))) {
////            return;
////        }
////
////        throw new UnauthorizedAccessException("You don't have permission to access this lead");
////    }
////
////    @Override
////    public List<LeadResponseDto> getAllLeads(String authHeader) {
////        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
////            throw new UnauthorizedAccessException("Only admins can access all leads");
////        }
////        return leadRepository.findAll().stream()
////                .map(leadMapper::toDto)
////                .peek(d -> {
////                    d.setLeadOwnerMeta(buildEmployeeMeta(d.getLeadOwner(), authHeader));
////                    d.setAddedByMeta(buildEmployeeMeta(d.getAddedBy(), authHeader));
////                })
////                .toList();
////    }
////
////    @Override
////    public List<LeadResponseDto> getMyLeads(String authHeader) {
////        String token = extractToken(authHeader);
////        String currentEmployeeId = jwtUtil.extractSubject(token);
////
////        return leadRepository.findByLeadOwnerOrAddedBy(currentEmployeeId, currentEmployeeId).stream()
////                .map(leadMapper::toDto)
////                .peek(d -> {
////                    d.setLeadOwnerMeta(buildEmployeeMeta(d.getLeadOwner(), authHeader));
////                    d.setAddedByMeta(buildEmployeeMeta(d.getAddedBy(), authHeader));
////                })
////                .toList();
////    }
////
////    @Override
////    @Transactional
////    public LeadResponseDto updateLead(Long id, LeadRequestDto dto, String authHeader) {
////        Lead lead = leadRepository.findById(id)
////                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
////
////        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
////            throw new UnauthorizedAccessException("Only admins can update leads");
////        }
////
////        if (dto.getEmail() != null && !dto.getEmail().equals(lead.getEmail())) {
////            if (leadRepository.existsByEmail(dto.getEmail())) throw new DuplicateResourceException("Lead with this email already exists");
////            lead.setEmail(dto.getEmail());
////        }
////        if (dto.getMobileNumber() != null && !dto.getMobileNumber().equals(lead.getMobileNumber())) {
////            if (leadRepository.existsByMobileNumber(dto.getMobileNumber())) throw new DuplicateResourceException("Lead with this mobile number already exists");
////            lead.setMobileNumber(dto.getMobileNumber());
////        }
////
////        if (dto.getName() != null) lead.setName(dto.getName());
////        if (dto.getClientCategory() != null) lead.setClientCategory(dto.getClientCategory());
////        if (dto.getLeadSource() != null) lead.setLeadSource(dto.getLeadSource());
////        if (dto.getLeadOwner() != null) {
////            validateEmployeeExists(dto.getLeadOwner(), authHeader);
////            lead.setLeadOwner(dto.getLeadOwner());
////        }
////        if (dto.getAddedBy() != null) {
////            validateEmployeeExists(dto.getAddedBy(), authHeader);
////            lead.setAddedBy(dto.getAddedBy());
////        }
////        if (dto.getCreateDeal() != null) lead.setCreateDeal(dto.getCreateDeal());
////        if (dto.getAutoConvertToClient() != null) lead.setAutoConvertToClient(dto.getAutoConvertToClient());
////        if (dto.getCompanyName() != null) lead.setCompanyName(dto.getCompanyName());
////        if (dto.getOfficialWebsite() != null) lead.setOfficialWebsite(dto.getOfficialWebsite());
////        if (dto.getOfficePhone() != null) lead.setOfficePhone(dto.getOfficePhone());
////        if (dto.getCity() != null) lead.setCity(dto.getCity());
////        if (dto.getState() != null) lead.setState(dto.getState());
////        if (dto.getPostalCode() != null) lead.setPostalCode(dto.getPostalCode());
////        if (dto.getCountry() != null) lead.setCountry(dto.getCountry());
////        if (dto.getCompanyAddress() != null) lead.setCompanyAddress(dto.getCompanyAddress());
////
////        lead = leadRepository.save(lead);
////        return leadMapper.toDto(lead);
////    }
////
////    @Override
////    @Transactional
////    public void deleteLead(Long id, String authHeader) {
////        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
////            throw new UnauthorizedAccessException("Only admins can delete leads");
////        }
////        if (!leadRepository.existsById(id)) {
////            throw new ResourceNotFoundException("Lead not found with ID: " + id);
////        }
////        leadRepository.deleteById(id);
////    }
////
////    // ------- helper methods -------
////
////    private void validateEmployeeExists(String employeeId, String authHeader) {
////        try {
////            Boolean exists = employeeClient.checkEmployeeExists(employeeId, authHeader);
////            if (exists == null || !exists) {
////                throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
////            }
////        } catch (FeignException.NotFound e) {
////            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
////        } catch (FeignException e) {
////            log.error("Feign error validating employee {}: status={}, body={}", employeeId, e.status(), safeContent(e));
////            throw new RuntimeException("Error validating employee: " + e.getMessage());
////        } catch (Exception e) {
////            log.error("Unexpected error validating employee {}", employeeId, e);
////            throw new RuntimeException("Unexpected error validating employee");
////        }
////    }
////
////    private String safeContent(FeignException e) {
////        try {
////            return e.contentUTF8();
////        } catch (Exception ex) {
////            return "<no content>";
////        }
////    }
////
////    private String extractToken(String authHeader) {
////        if (authHeader == null) return null;
////        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
////    }
////
////    /**
////     * Keep autoConvertLeadToClient for compatibility/test, but production flow prefers the AFTER_COMMIT listener.
////     * This method now uses the multipart-feign contract: it converts DTO -> JSON string and calls clientClient.createClient(clientJson, null, null, auth)
////     */
////    private void autoConvertLeadToClient(Long leadId, String authHeader) {
////        try {
////            Lead lead = leadRepository.findById(leadId).orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
////            List<ClientResponseDto> existing = null;
////            try {
////                existing = clientClient.getClientsByEmail(lead.getEmail(), authHeader);
////            } catch (FeignException fe) {
////                log.warn("Feign error checking clients by email for lead {}: {}", leadId, safeContent(fe));
////            }
////
////            if (existing != null && !existing.isEmpty()) {
////                log.info("Client already exists with email {}, skipping conversion", lead.getEmail());
////                try {
////                    lead.setStatus(CONVERTED);
////                } catch (Exception ex) {
////                    lead.setStatus(CONVERTED);
////                }
////                leadRepository.save(lead);
////                return;
////            }
////
////            ClientRequestDto clientDto = new ClientRequestDto();
////            clientDto.setName(lead.getName());
////            clientDto.setEmail(lead.getEmail());
////            clientDto.setMobile(lead.getMobileNumber());
////            clientDto.setCountry(lead.getCountry());
////            clientDto.setCategory(lead.getClientCategory());
////            clientDto.setCompanyName(lead.getCompanyName());
////            clientDto.setWebsite(lead.getOfficialWebsite());
////
////            String clientJson;
////            try {
////                clientJson = objectMapper.writeValueAsString(clientDto);
////            } catch (Exception e) {
////                log.error("Failed to serialize client DTO for lead {}: {}", leadId, e.getMessage(), e);
////                return;
////            }
////
////            try {
////                ClientResponseDto created = clientClient.createClient(clientJson, null, null, authHeader);
////                if (created != null) {
////                    try {
////                        lead.setStatus(LeadStatus.CONVERTED);
////                    } catch (Exception ex) {
////                        lead.setStatus(LeadStatus.CONVERTED);
////                    }
////                    leadRepository.save(lead);
////                    log.info("Lead {} converted to client {}", leadId, created.getId());
////                } else {
////                    log.warn("Client service returned null while creating client for lead {}", leadId);
////                }
////            } catch (FeignException fe) {
////                log.error("Feign error converting lead to client: status={}, body={}", fe.status(), safeContent(fe));
////            } catch (Exception e) {
////                log.error("Error converting lead to client: {}", e.getMessage(), e);
////            }
////
////        } catch (ResourceNotFoundException rnfe) {
////            log.warn("Lead not found during auto-convert: {}", leadId);
////        } catch (Exception e) {
////            log.error("Unexpected error in autoConvertLeadToClient for lead {}: {}", leadId, e.getMessage(), e);
////        }
////    }
////
////    private EmployeeMetaDto buildEmployeeMeta(String employeeId, String authHeader) {
////        if (employeeId == null || employeeId.isBlank()) return null;
////        try {
////            var emp = employeeClient.getEmployeeById(employeeId, authHeader);
////            return toMeta(emp);
////        } catch (feign.FeignException.Forbidden | feign.FeignException.Unauthorized ex) {
////            // fallback: हल्का/इंटरनल रास्ता
////            try {
////                // OPTION A: public meta endpoint (no auth)
////                var meta = employeeClient.getMeta(employeeId);
////                return meta;
////                // OPTION B: internal key fallback
////                // var emp = employeeClient.getEmployeeByIdInternal(employeeId, internalApiKey);
////                // return toMeta(emp);
////            } catch (Exception e2) {
////                log.warn("Fallback meta fetch failed for {}: {}", employeeId, e2.getMessage());
////                return null;
////            }
////        } catch (Exception e) {
////            log.warn("Meta fetch failed for {}: {}", employeeId, e.getMessage());
////            return null;
////        }
////    }
////
////    private EmployeeMetaDto toMeta(EmployeeDto emp) {
////        if (emp == null) return null;
////        EmployeeMetaDto m = new EmployeeMetaDto();
////        m.setEmployeeId(emp.getEmployeeId());
////        m.setName(emp.getName());
////        m.setDesignation(emp.getDesignationName());
////        m.setDepartment(emp.getDepartmentName());
////        m.setProfileUrl(emp.getProfilePictureUrl());
////        return m;
////    }
////
////}
//package com.erp.lead_service.service;
//
//import com.erp.lead_service.client.ClientServiceClient;
//import com.erp.lead_service.client.EmployeeServiceClient;
//import com.erp.lead_service.client.NotificationClient;
//import com.erp.lead_service.dto.EmployeeMetaDto;
//import com.erp.lead_service.dto.Import.ImportResult;
//import com.erp.lead_service.dto.Import.LeadImport;
//import com.erp.lead_service.dto.client.ClientRequestDto;
//import com.erp.lead_service.dto.client.ClientResponseDto;
//import com.erp.lead_service.dto.deal.DealRequestDto;
//import com.erp.lead_service.dto.employee.EmployeeDto;
//import com.erp.lead_service.dto.lead.DealMiniDto;
//import com.erp.lead_service.dto.lead.LeadDealStatsDto;
//import com.erp.lead_service.dto.lead.LeadRequestDto;
//import com.erp.lead_service.dto.lead.LeadResponseDto;
//import com.erp.lead_service.entity.Deal;
//import com.erp.lead_service.entity.Lead;
//import com.erp.lead_service.entity.LeadStatus;
//import com.erp.lead_service.exception.DuplicateResourceException;
//import com.erp.lead_service.exception.ResourceNotFoundException;
//import com.erp.lead_service.exception.UnauthorizedAccessException;
//import com.erp.lead_service.mapper.DealMapper;
//import com.erp.lead_service.mapper.LeadMapper;
//import com.erp.lead_service.repository.DealRepository;
//import com.erp.lead_service.repository.LeadRepository;
//import com.erp.lead_service.util.JwtUtil;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import feign.FeignException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.csv.CSVFormat;
//import org.apache.commons.csv.CSVParser;
//import org.apache.commons.csv.CSVRecord;
//import org.springframework.context.ApplicationEventPublisher;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.InputStreamReader;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import java.util.*;
//
//import static com.erp.lead_service.entity.LeadStatus.CONVERTED;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class LeadServiceImpl implements LeadService {
//
//    private final LeadRepository leadRepository;
//    private final LeadMapper leadMapper;
//    private final EmployeeServiceClient employeeClient;
//    private final JwtUtil jwtUtil;
//    private final DealService dealService;
//    private final DealMapper dealMapper;
//    private final ClientServiceClient clientClient;
//    private final NotificationClient notificationClient;
//    private final DealRepository dealRepository;
//    private final ApplicationEventPublisher eventPublisher;
//    private final ObjectMapper objectMapper;
//
//    // ---------------- existing business methods (unchanged) ----------------
//
//    @Override
//    @Transactional
//    public LeadResponseDto createLead(LeadRequestDto dto, String authHeader) {
//        if (dto.getEmail() != null && leadRepository.existsByEmail(dto.getEmail())) {
//            throw new DuplicateResourceException("Lead with this email already exists");
//        }
//        if (dto.getMobileNumber() != null && leadRepository.existsByMobileNumber(dto.getMobileNumber())) {
//            throw new DuplicateResourceException("Lead with this mobile number already exists");
//        }
//
//        String token = extractToken(authHeader);
//        String currentEmployeeId = jwtUtil.extractSubject(token);
//        boolean isAdmin = jwtUtil.isAdmin(token);
//
//        if (!isAdmin) {
//            dto.setLeadOwner(currentEmployeeId);
//            dto.setAddedBy(currentEmployeeId);
//        } else {
//            if (dto.getLeadOwner() != null) validateEmployeeExists(dto.getLeadOwner(), authHeader);
//            if (dto.getAddedBy() != null) validateEmployeeExists(dto.getAddedBy(), authHeader);
//        }
//
//        Lead lead = leadMapper.toEntity(dto);
//        lead.setAddedBy(dto.getAddedBy() != null ? dto.getAddedBy() : currentEmployeeId);
//        lead.setLeadOwner(dto.getLeadOwner() != null ? dto.getLeadOwner() : currentEmployeeId);
//
//        Lead saved = leadRepository.save(lead);
//        log.info("Lead created with id {}", saved.getId());
//
//        boolean shouldAttemptAutoConvert = Boolean.TRUE.equals(saved.getAutoConvertToClient());
//
//        if (dto.getCreateDeal() != null && dto.getCreateDeal() && dto.getDeal() != null) {
//            try {
//                DealMiniDto d = dto.getDeal();
//                DealRequestDto dealDto = new DealRequestDto();
//                dealDto.setTitle(d.getTitle());
//                dealDto.setPipeline(d.getPipeline());
//                dealDto.setDealStage(d.getDealStage());
//                dealDto.setDealCategory(d.getDealCategory());
//                dealDto.setValue(d.getValue());
//                dealDto.setExpectedCloseDate(d.getExpectedCloseDate());
//                dealDto.setDealAgent(d.getDealAgent());
//                dealDto.setDealWatchers(d.getDealWatchers());
//                dealDto.setDealContact(saved.getName());
//                eventPublisher.publishEvent(new com.erp.lead_service.event.LeadCreateDealEvent(saved.getId(), dealDto, authHeader));
//                log.info("Published LeadCreateDealEvent for lead {}", saved.getId());
//            } catch (Exception ex) {
//                log.error("Failed to publish LeadCreateDealEvent for lead {}: {}", saved.getId(), ex.getMessage(), ex);
//            }
//        }
//
//        if (shouldAttemptAutoConvert) {
//            try {
//                eventPublisher.publishEvent(new com.erp.lead_service.event.LeadAutoConvertEvent(saved.getId(), authHeader));
//                log.info("Published LeadAutoConvertEvent for lead {}", saved.getId());
//            } catch (Exception e) {
//                log.warn("Failed to publish LeadAutoConvertEvent for lead {}: {}", saved.getId(), e.getMessage(), e);
//            }
//        }
//
//        return leadMapper.toDto(saved);
//    }
//
//    @Override
//    public LeadResponseDto getLeadById(Long id, String authHeader) {
//        Lead lead = leadRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
//        checkLeadAccess(lead, authHeader);
//
//        LeadResponseDto out = leadMapper.toDto(lead);
//        out.setLeadOwnerMeta(buildEmployeeMeta(out.getLeadOwner(), authHeader));
//        out.setAddedByMeta(buildEmployeeMeta(out.getAddedBy(), authHeader));
//        return out;
//    }
//
//    private void checkLeadAccess(Lead lead, String authHeader) {
//        String token = extractToken(authHeader);
//        String currentEmployeeId = jwtUtil.extractSubject(token);
//        boolean isAdmin = jwtUtil.isAdmin(token);
//
//        if (isAdmin) return;
//
//        if (lead == null) throw new ResourceNotFoundException("Lead not found");
//
//        String owner = lead.getLeadOwner();
//        String addedBy = lead.getAddedBy();
//
//        if ((owner != null && owner.equals(currentEmployeeId)) ||
//                (addedBy != null && addedBy.equals(currentEmployeeId))) {
//            return;
//        }
//
//        throw new UnauthorizedAccessException("You don't have permission to access this lead");
//    }
//
//    @Override
//    public List<LeadResponseDto> getAllLeads(String authHeader) {
//        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
//            throw new UnauthorizedAccessException("Only admins can access all leads");
//        }
//        return leadRepository.findAll().stream()
//                .map(leadMapper::toDto)
//                .peek(d -> {
//                    d.setLeadOwnerMeta(buildEmployeeMeta(d.getLeadOwner(), authHeader));
//                    d.setAddedByMeta(buildEmployeeMeta(d.getAddedBy(), authHeader));
//                })
//                .toList();
//    }
//
//    @Override
//    public List<LeadResponseDto> getMyLeads(String authHeader) {
//        String token = extractToken(authHeader);
//        String currentEmployeeId = jwtUtil.extractSubject(token);
//
//        return leadRepository.findByLeadOwnerOrAddedBy(currentEmployeeId, currentEmployeeId).stream()
//                .map(leadMapper::toDto)
//                .peek(d -> {
//                    d.setLeadOwnerMeta(buildEmployeeMeta(d.getLeadOwner(), authHeader));
//                    d.setAddedByMeta(buildEmployeeMeta(d.getAddedBy(), authHeader));
//                })
//                .toList();
//    }
//
//    @Override
//    @Transactional
//    public LeadResponseDto updateLead(Long id, LeadRequestDto dto, String authHeader) {
//        Lead lead = leadRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
//
//        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
//            throw new UnauthorizedAccessException("Only admins can update leads");
//        }
//
//        if (dto.getEmail() != null && !dto.getEmail().equals(lead.getEmail())) {
//            if (leadRepository.existsByEmail(dto.getEmail())) throw new DuplicateResourceException("Lead with this email already exists");
//            lead.setEmail(dto.getEmail());
//        }
//        if (dto.getMobileNumber() != null && !dto.getMobileNumber().equals(lead.getMobileNumber())) {
//            if (leadRepository.existsByMobileNumber(dto.getMobileNumber())) throw new DuplicateResourceException("Lead with this mobile number already exists");
//            lead.setMobileNumber(dto.getMobileNumber());
//        }
//
//        if (dto.getName() != null) lead.setName(dto.getName());
//        if (dto.getClientCategory() != null) lead.setClientCategory(dto.getClientCategory());
//        if (dto.getLeadSource() != null) lead.setLeadSource(dto.getLeadSource());
//        if (dto.getLeadOwner() != null) {
//            validateEmployeeExists(dto.getLeadOwner(), authHeader);
//            lead.setLeadOwner(dto.getLeadOwner());
//        }
//        if (dto.getAddedBy() != null) {
//            validateEmployeeExists(dto.getAddedBy(), authHeader);
//            lead.setAddedBy(dto.getAddedBy());
//        }
//        if (dto.getCreateDeal() != null) lead.setCreateDeal(dto.getCreateDeal());
//        if (dto.getAutoConvertToClient() != null) lead.setAutoConvertToClient(dto.getAutoConvertToClient());
//        if (dto.getCompanyName() != null) lead.setCompanyName(dto.getCompanyName());
//        if (dto.getOfficialWebsite() != null) lead.setOfficialWebsite(dto.getOfficialWebsite());
//        if (dto.getOfficePhone() != null) lead.setOfficePhone(dto.getOfficePhone());
//        if (dto.getCity() != null) lead.setCity(dto.getCity());
//        if (dto.getState() != null) lead.setState(dto.getState());
//        if (dto.getPostalCode() != null) lead.setPostalCode(dto.getPostalCode());
//        if (dto.getCountry() != null) lead.setCountry(dto.getCountry());
//        if (dto.getCompanyAddress() != null) lead.setCompanyAddress(dto.getCompanyAddress());
//
//        lead = leadRepository.save(lead);
//        return leadMapper.toDto(lead);
//    }
//
//    @Override
//    @Transactional
//    public void deleteLead(Long id, String authHeader) {
//        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
//            throw new UnauthorizedAccessException("Only admins can delete leads");
//        }
//        if (!leadRepository.existsById(id)) {
//            throw new ResourceNotFoundException("Lead not found with ID: " + id);
//        }
//        leadRepository.deleteById(id);
//    }
//
//    // ---------------- CSV import enhancements for Lead ----------------
//
//    @Override
//    @Transactional
//    public List<ImportResult> importLeadsFromCsv(MultipartFile file, String authHeader) {
//        List<ImportResult> results = new ArrayList<>();
//        if (file == null || file.isEmpty()) {
//            results.add(new ImportResult(0, "ERROR", "Empty or missing file", null));
//            return results;
//        }
//
//        String token = extractToken(authHeader);
//        String currentEmployeeId = jwtUtil.extractSubject(token);
//        boolean isAdmin = jwtUtil.isAdmin(token);
//
//        // pre-load existing leads for duplicate mobile checks (ok for moderate datasets)
//        List<Lead> existingLeads = leadRepository.findAll();
//
//        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
//            CSVParser parser = CSVFormat.DEFAULT
//                    .withFirstRecordAsHeader()
//                    .withTrim(false)
//                    .withAllowMissingColumnNames() // important to ignore trailing empty headers
//                    .parse(reader);
//
//            // build normalized header map (normalized -> actual header name)
//            Map<String, String> headerMap = new HashMap<>();
//            for (String raw : parser.getHeaderMap().keySet()) {
//                if (raw == null) continue;
//                String nk = normalizeHeaderKey(raw);
//                if (nk.isBlank()) {
//                    log.debug("Ignoring blank header column (raw='{}')", raw);
//                    continue;
//                }
//                if (headerMap.containsKey(nk)) {
//                    log.warn("Duplicate normalized CSV header '{}' found (keeping first: '{}' ; ignoring: '{}')", nk, headerMap.get(nk), raw);
//                    continue;
//                }
//                headerMap.put(nk, raw);
//            }
//
//            int rowCounter = 0;
//            for (CSVRecord record : parser) {
//                rowCounter = (int) record.getRecordNumber() + 1;
//                try {
//                    LeadImport imp = mapRecordToLeadImport(record, headerMap);
//
//                    // normalize fields
//                    String name = safeTrim(imp.getName());
//                    String emailRaw = safeTrim(imp.getEmail());
//                    String email = emailRaw == null ? null : emailRaw.toLowerCase();
//                    String mobileRaw = safeTrim(imp.getMobileNumber());
//                    String mobileNormalized = normalizeMobileDigits(mobileRaw);
//
//                    // skip empty rows
//                    if ((name == null || name.isBlank()) && (email == null || email.isBlank()) && (mobileRaw == null || mobileRaw.isBlank())) {
//                        results.add(new ImportResult(rowCounter, "SKIPPED", "Empty row (no name/email/mobile)", null));
//                        continue;
//                    }
//
//                    // duplicate checks
//                    boolean emailExists = false;
//                    if (email != null && !email.isBlank()) {
//                        emailExists = leadRepository.existsByEmailIgnoreCase(email);
//                    }
//
//                    boolean mobileExists = false;
//                    if (mobileNormalized != null && !mobileNormalized.isBlank()) {
//                        for (Lead ex : existingLeads) {
//                            String exMob = normalizeMobileDigits(ex.getMobileNumber());
//                            if (exMob != null && exMob.equals(mobileNormalized)) {
//                                mobileExists = true;
//                                break;
//                            }
//                        }
//                    }
//
//                    if (emailExists) {
//                        results.add(new ImportResult(rowCounter, "SKIPPED", "Duplicate email", null));
//                        continue;
//                    }
//                    if (mobileExists) {
//                        results.add(new ImportResult(rowCounter, "SKIPPED", "Duplicate mobile", null));
//                        continue;
//                    }
//
//                    // create entity
//                    Lead lead = new Lead();
//                    lead.setName(name);
//                    lead.setEmail(email);
//                    lead.setCompanyName(safeTrim(imp.getCompanyName()));
//                    lead.setOfficialWebsite(safeTrim(imp.getOfficialWebsite()));
//                    lead.setMobileNumber(safeTrim(imp.getMobileNumber()));
//                    lead.setOfficePhone(safeTrim(imp.getOfficePhone()));
//                    lead.setCity(safeTrim(imp.getCity()));
//                    lead.setState(safeTrim(imp.getState()));
//                    lead.setPostalCode(safeTrim(imp.getPostalCode()));
//                    lead.setCountry(safeTrim(imp.getCountry()));
//                    lead.setCompanyAddress(safeTrim(imp.getCompanyAddress()));
//
//                    if (!isAdmin) {
//                        lead.setLeadOwner(currentEmployeeId);
//                        lead.setAddedBy(currentEmployeeId);
//                    } else {
//                        lead.setAddedBy(currentEmployeeId);
//                        lead.setLeadOwner(currentEmployeeId);
//                    }
//
//                    Lead saved = leadRepository.save(lead);
//                    existingLeads.add(saved);
//                    results.add(new ImportResult(rowCounter, "CREATED", null, saved.getId()));
//
//                } catch (Exception e) {
//                    log.error("Row import failed at {}: {}", rowCounter, safeMessage(e), e);
//                    results.add(new ImportResult(rowCounter, "ERROR", "Parse or save error: " + safeMessage(e), null));
//                }
//            }
//
//        } catch (Exception ex) {
//            log.error("Failed to parse file: {}", safeMessage(ex), ex);
//            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(ex), null));
//        }
//
//        return results;
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public LeadDealStatsDto getLeadDealStats(Long leadId, String authHeader) {
//        // Ensure lead exists & access allowed
//        Lead lead = leadRepository.findById(leadId)
//                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + leadId));
//
//        // Reuse existing access check (throws if not allowed)
//        checkLeadAccess(lead, authHeader);
//
//        Long total = 0L;
//        Long wins = 0L;
//        try {
//            total = dealRepository.countByLeadId(leadId);
//            wins = dealRepository.countWinsByLeadId(leadId);
//        } catch (Exception ex) {
//            log.warn("Failed to get counts from repository for lead {}: {} - falling back to list scan", leadId, ex.getMessage());
//            // fallback: compute via list
//            List<Deal> deals = dealRepository.findByLeadId(leadId);
//            total = (long) deals.size();
//            wins = deals.stream()
//                    .filter(d -> d.getDealStage() != null && "win".equalsIgnoreCase(d.getDealStage()))
//                    .count();
//        }
//
//        return LeadDealStatsDto.builder()
//                .leadId(leadId)
//                .totalDeals(total == null ? 0L : total)
//                .convertedDeals(wins == null ? 0L : wins)
//                .build();
//    }
//
//
//    // ---------------- helper / mapping functions ----------------
//
//    private static String normalizeHeaderKey(String header) {
//        if (header == null) return "";
//        return header.trim().toLowerCase().replaceAll("\\s+", "");
//    }
//
//    private LeadImport mapRecordToLeadImport(CSVRecord record, Map<String, String> headerMapping) {
//        LeadImport imp = new LeadImport();
//
//        java.util.function.Function<String[], String> valueOf = (keys) -> {
//            for (String k : keys) {
//                String nk = normalizeHeaderKey(k);
//                String actual = headerMapping.get(nk);
//                if (actual != null && record.isMapped(actual)) {
//                    String v = record.get(actual);
//                    if (v != null) return v.trim();
//                }
//            }
//            return null;
//        };
//
//        imp.setName(valueOf.apply(new String[]{"name","full name","contact name"}));
//        imp.setEmail(valueOf.apply(new String[]{"email","e-mail","email address"}));
//        imp.setCompanyName(valueOf.apply(new String[]{"company","company name","organization"}));
//        imp.setOfficialWebsite(valueOf.apply(new String[]{"website","official website","site","url"}));
//        imp.setMobileNumber(valueOf.apply(new String[]{"mobile","mobile number","phone","phone number","mobile_no","mobile_no."}));
//        imp.setOfficePhone(valueOf.apply(new String[]{"officephone","office phone","office_phone","phone_office","landline"}));
//        imp.setCity(valueOf.apply(new String[]{"city"}));
//        imp.setState(valueOf.apply(new String[]{"state","region","province"}));
//        imp.setPostalCode(valueOf.apply(new String[]{"postalcode","postal code","zip","zip code"}));
//        imp.setCountry(valueOf.apply(new String[]{"country"}));
//        imp.setCompanyAddress(valueOf.apply(new String[]{"address","company address","office address","company_address"}));
//        // optional date column
//        return imp;
//    }
//
//    private static String safeTrim(String s) { return s == null ? null : s.trim(); }
//
//    private static String normalizeMobileDigits(String mobile) {
//        if (mobile == null) return null;
//        String digits = mobile.replaceAll("\\D+", "");
//        return digits.isBlank() ? null : digits;
//    }
//
//    private static String safeMessage(Exception e) { return e == null ? "" : (e.getMessage() == null ? e.toString() : e.getMessage()); }
//
//    // Flexible datetime parser — supports many common formats (date-only or date+time)
//    private LocalDateTime parseFlexibleDateTime(String input) {
//        if (input == null || input.isBlank()) return null;
//
//        List<String> patterns = List.of(
//                "yyyy-MM-dd'T'HH:mm:ss",
//                "yyyy-MM-dd HH:mm:ss",
//                "yyyy-MM-dd",
//                "dd-MM-yyyy",
//                "dd/MM/yyyy",
//                "MM/dd/yyyy",
//                "d MMM yyyy",
//                "dd MMM yyyy",
//                "yyyy/MM/dd"
//        );
//
//        for (String p : patterns) {
//            try {
//                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(p);
//                if (p.contains("H") || p.contains("T")) {
//                    return LocalDateTime.parse(input, fmt);
//                } else {
//                    LocalDate d = LocalDate.parse(input, fmt);
//                    return d.atStartOfDay();
//                }
//            } catch (DateTimeParseException ignored) {
//            }
//        }
//        return null;
//    }
//
//    // ---------------- existing helper methods (unchanged) ----------------
//    private void validateEmployeeExists(String employeeId, String authHeader) {
//        try {
//            Boolean exists = employeeClient.checkEmployeeExists(employeeId, authHeader);
//            if (exists == null || !exists) {
//                throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
//            }
//        } catch (FeignException.NotFound e) {
//            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
//        } catch (FeignException e) {
//            log.error("Feign error validating employee {}: status={}, body={}", employeeId, e.status(), safeContent(e));
//            throw new RuntimeException("Error validating employee: " + e.getMessage());
//        } catch (Exception e) {
//            log.error("Unexpected error validating employee {}: {}", employeeId, e.getMessage());
//            throw new RuntimeException("Unexpected error validating employee");
//        }
//    }
//
//    private String safeContent(FeignException e) {
//        try { return e.contentUTF8(); } catch (Exception ex) { return "<no content>"; }
//    }
//
//    private String extractToken(String authHeader) {
//        if (authHeader == null) return null;
//        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
//    }
//
//    private void autoConvertLeadToClient(Long leadId, String authHeader) {
//        try {
//            Lead lead = leadRepository.findById(leadId).orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
//            List<ClientResponseDto> existing = null;
//            try {
//                existing = clientClient.getClientsByEmail(lead.getEmail(), authHeader);
//            } catch (FeignException fe) {
//                log.warn("Feign error checking clients by email for lead {}: {}", leadId, safeContent(fe));
//            }
//
//            if (existing != null && !existing.isEmpty()) {
//                log.info("Client already exists with email {}, skipping conversion", lead.getEmail());
//                try { lead.setStatus(CONVERTED); } catch (Exception ex) { lead.setStatus(CONVERTED); }
//                leadRepository.save(lead);
//                return;
//            }
//
//            ClientRequestDto clientDto = new ClientRequestDto();
//            clientDto.setName(lead.getName());
//            clientDto.setEmail(lead.getEmail());
//            clientDto.setMobile(lead.getMobileNumber());
//            clientDto.setCountry(lead.getCountry());
//            clientDto.setCategory(lead.getClientCategory());
//            clientDto.setCompanyName(lead.getCompanyName());
//            clientDto.setWebsite(lead.getOfficialWebsite());
//
//            String clientJson;
//            try {
//                clientJson = objectMapper.writeValueAsString(clientDto);
//            } catch (Exception e) {
//                log.error("Failed to serialize client DTO for lead {}: {}", leadId, e.getMessage(), e);
//                return;
//            }
//
//            try {
//                ClientResponseDto created = clientClient.createClient(clientJson, null, null, authHeader);
//                if (created != null) {
//                    try { lead.setStatus(LeadStatus.CONVERTED); } catch (Exception ex) { lead.setStatus(LeadStatus.CONVERTED); }
//                    leadRepository.save(lead);
//                    log.info("Lead {} converted to client {}", leadId, created.getId());
//                } else {
//                    log.warn("Client service returned null while creating client for lead {}", leadId);
//                }
//            } catch (FeignException fe) {
//                log.error("Feign error converting lead to client: status={}, body={}", fe.status(), safeContent(fe));
//            } catch (Exception e) {
//                log.error("Error converting lead to client: {}", e.getMessage(), e);
//            }
//
//        } catch (ResourceNotFoundException rnfe) {
//            log.warn("Lead not found during auto-convert: {}", leadId);
//        } catch (Exception e) {
//            log.error("Unexpected error in autoConvertLeadToClient for lead {}: {}", leadId, e.getMessage(), e);
//        }
//    }
//
//    private EmployeeMetaDto buildEmployeeMeta(String employeeId, String authHeader) {
//        if (employeeId == null || employeeId.isBlank()) return null;
//        try {
//            var emp = employeeClient.getEmployeeById(employeeId, authHeader);
//            return toMeta(emp);
//        } catch (feign.FeignException.Forbidden | feign.FeignException.Unauthorized ex) {
//            try {
//                var meta = employeeClient.getMeta(employeeId);
//                return meta;
//            } catch (Exception e2) {
//                log.warn("Fallback meta fetch failed for {}: {}", employeeId, e2.getMessage());
//                return null;
//            }
//        } catch (Exception e) {
//            log.warn("Meta fetch failed for {}: {}", employeeId, e.getMessage());
//            return null;
//        }
//    }
//
//    private EmployeeMetaDto toMeta(EmployeeDto emp) {
//        if (emp == null) return null;
//        EmployeeMetaDto m = new EmployeeMetaDto();
//        m.setEmployeeId(emp.getEmployeeId());
//        m.setName(emp.getName());
//        m.setDesignation(emp.getDesignationName());
//        m.setDepartment(emp.getDepartmentName());
//        m.setProfileUrl(emp.getProfilePictureUrl());
//        return m;
//    }
//
//}


package com.erp.lead_service.service;

import com.erp.lead_service.client.ClientServiceClient;
import com.erp.lead_service.client.EmployeeServiceClient;
import com.erp.lead_service.client.NotificationClient;
import com.erp.lead_service.dto.EmployeeMetaDto;
import com.erp.lead_service.dto.Import.ImportResult;
import com.erp.lead_service.dto.Import.LeadImport;
import com.erp.lead_service.dto.client.ClientRequestDto;
import com.erp.lead_service.dto.client.ClientResponseDto;
import com.erp.lead_service.dto.deal.DealRequestDto;
import com.erp.lead_service.dto.employee.EmployeeDto;
import com.erp.lead_service.dto.lead.DealMiniDto;
import com.erp.lead_service.dto.lead.LeadDealStatsDto;
import com.erp.lead_service.dto.lead.LeadRequestDto;
import com.erp.lead_service.dto.lead.LeadResponseDto;
import com.erp.lead_service.entity.Deal;
import com.erp.lead_service.entity.Lead;
import com.erp.lead_service.entity.LeadStatus;
import com.erp.lead_service.exception.DuplicateResourceException;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.mapper.DealMapper;
import com.erp.lead_service.mapper.LeadMapper;
import com.erp.lead_service.repository.DealRepository;
import com.erp.lead_service.repository.LeadRepository;
import com.erp.lead_service.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.erp.lead_service.entity.LeadStatus.CONVERTED;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final LeadMapper leadMapper;
    private final EmployeeServiceClient employeeClient;
    private final JwtUtil jwtUtil;
    private final DealService dealService;
    private final DealMapper dealMapper;
    private final ClientServiceClient clientClient;
    private final NotificationClient notificationClient;
    private final DealRepository dealRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // ---------------- existing business methods (unchanged except removal of direct auto-convert publish) ----------------

    @Override
    @Transactional
    public LeadResponseDto createLead(LeadRequestDto dto, String authHeader) {
        if (dto.getEmail() != null && leadRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Lead with this email already exists");
        }
        if (dto.getMobileNumber() != null && leadRepository.existsByMobileNumber(dto.getMobileNumber())) {
            throw new DuplicateResourceException("Lead with this mobile number already exists");
        }

        String token = extractToken(authHeader);
        String currentEmployeeId = jwtUtil.extractSubject(token);
        boolean isAdmin = jwtUtil.isAdmin(token);

        if (!isAdmin) {
            dto.setLeadOwner(currentEmployeeId);
            dto.setAddedBy(currentEmployeeId);
        } else {
            if (dto.getLeadOwner() != null) validateEmployeeExists(dto.getLeadOwner(), authHeader);
            if (dto.getAddedBy() != null) validateEmployeeExists(dto.getAddedBy(), authHeader);
        }

        Lead lead = leadMapper.toEntity(dto);
        lead.setAddedBy(dto.getAddedBy() != null ? dto.getAddedBy() : currentEmployeeId);
        lead.setLeadOwner(dto.getLeadOwner() != null ? dto.getLeadOwner() : currentEmployeeId);

        Lead saved = leadRepository.save(lead);
        log.info("Lead created with id {}", saved.getId());

        // IMPORTANT CHANGE: DO NOT publish LeadAutoConvertEvent here.
        // Auto-conversion to client should only be triggered from deal-related flows when a deal's stage becomes WIN
        // and the lead.autoConvertToClient flag is true. Those flows already publish LeadAutoConvertEvent conditionally.

        // If client explicitly requested to create deal, publish an AFTER_COMMIT event to create it.
        if (dto.getCreateDeal() != null && dto.getCreateDeal() && dto.getDeal() != null) {
            try {
                DealMiniDto d = dto.getDeal();
                DealRequestDto dealDto = new DealRequestDto();
                dealDto.setTitle(d.getTitle());
                dealDto.setPipeline(d.getPipeline());
                dealDto.setDealStage(d.getDealStage());
                dealDto.setDealCategory(d.getDealCategory());
                dealDto.setValue(d.getValue());
                dealDto.setExpectedCloseDate(d.getExpectedCloseDate());
                dealDto.setDealAgent(d.getDealAgent());
                dealDto.setDealWatchers(d.getDealWatchers());
                dealDto.setDealContact(saved.getName());
                eventPublisher.publishEvent(new com.erp.lead_service.event.LeadCreateDealEvent(saved.getId(), dealDto, authHeader));
                log.info("Published LeadCreateDealEvent for lead {}", saved.getId());
            } catch (Exception ex) {
                log.error("Failed to publish LeadCreateDealEvent for lead {}: {}", saved.getId(), ex.getMessage(), ex);
            }
        }

        return leadMapper.toDto(saved);
    }

    @Override
    public LeadResponseDto getLeadById(Long id, String authHeader) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
        checkLeadAccess(lead, authHeader);

        LeadResponseDto out = leadMapper.toDto(lead);
        out.setLeadOwnerMeta(buildEmployeeMeta(out.getLeadOwner(), authHeader));
        out.setAddedByMeta(buildEmployeeMeta(out.getAddedBy(), authHeader));
        return out;
    }

    private void checkLeadAccess(Lead lead, String authHeader) {
        String token = extractToken(authHeader);
        String currentEmployeeId = jwtUtil.extractSubject(token);
        boolean isAdmin = jwtUtil.isAdmin(token);

        if (isAdmin) return;

        if (lead == null) throw new ResourceNotFoundException("Lead not found");

        String owner = lead.getLeadOwner();
        String addedBy = lead.getAddedBy();

        if ((owner != null && owner.equals(currentEmployeeId)) ||
                (addedBy != null && addedBy.equals(currentEmployeeId))) {
            return;
        }

        throw new UnauthorizedAccessException("You don't have permission to access this lead");
    }

    @Override
    public List<LeadResponseDto> getAllLeads(String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access all leads");
        }
        return leadRepository.findAll().stream()
                .map(leadMapper::toDto)
                .peek(d -> {
                    d.setLeadOwnerMeta(buildEmployeeMeta(d.getLeadOwner(), authHeader));
                    d.setAddedByMeta(buildEmployeeMeta(d.getAddedBy(), authHeader));
                })
                .toList();
    }

    @Override
    public List<LeadResponseDto> getMyLeads(String authHeader) {
        String token = extractToken(authHeader);
        String currentEmployeeId = jwtUtil.extractSubject(token);

        return leadRepository.findByLeadOwnerOrAddedBy(currentEmployeeId, currentEmployeeId).stream()
                .map(leadMapper::toDto)
                .peek(d -> {
                    d.setLeadOwnerMeta(buildEmployeeMeta(d.getLeadOwner(), authHeader));
                    d.setAddedByMeta(buildEmployeeMeta(d.getAddedBy(), authHeader));
                })
                .toList();
    }

    @Override
    @Transactional
    public LeadResponseDto updateLead(Long id, LeadRequestDto dto, String authHeader) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));

        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can update leads");
        }

        if (dto.getEmail() != null && !dto.getEmail().equals(lead.getEmail())) {
            if (leadRepository.existsByEmail(dto.getEmail())) throw new DuplicateResourceException("Lead with this email already exists");
            lead.setEmail(dto.getEmail());
        }
        if (dto.getMobileNumber() != null && !dto.getMobileNumber().equals(lead.getMobileNumber())) {
            if (leadRepository.existsByMobileNumber(dto.getMobileNumber())) throw new DuplicateResourceException("Lead with this mobile number already exists");
            lead.setMobileNumber(dto.getMobileNumber());
        }

        if (dto.getName() != null) lead.setName(dto.getName());
        if (dto.getClientCategory() != null) lead.setClientCategory(dto.getClientCategory());
        if (dto.getLeadSource() != null) lead.setLeadSource(dto.getLeadSource());
        if (dto.getLeadOwner() != null) {
            validateEmployeeExists(dto.getLeadOwner(), authHeader);
            lead.setLeadOwner(dto.getLeadOwner());
        }
        if (dto.getAddedBy() != null) {
            validateEmployeeExists(dto.getAddedBy(), authHeader);
            lead.setAddedBy(dto.getAddedBy());
        }
        if (dto.getCreateDeal() != null) lead.setCreateDeal(dto.getCreateDeal());
        if (dto.getAutoConvertToClient() != null) lead.setAutoConvertToClient(dto.getAutoConvertToClient());
        if (dto.getCompanyName() != null) lead.setCompanyName(dto.getCompanyName());
        if (dto.getOfficialWebsite() != null) lead.setOfficialWebsite(dto.getOfficialWebsite());
        if (dto.getOfficePhone() != null) lead.setOfficePhone(dto.getOfficePhone());
        if (dto.getCity() != null) lead.setCity(dto.getCity());
        if (dto.getState() != null) lead.setState(dto.getState());
        if (dto.getPostalCode() != null) lead.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) lead.setCountry(dto.getCountry());
        if (dto.getCompanyAddress() != null) lead.setCompanyAddress(dto.getCompanyAddress());

        lead = leadRepository.save(lead);
        return leadMapper.toDto(lead);
    }

//    @Override
//    @Transactional
//    public void deleteLead(Long id, String authHeader) {
//        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
//            throw new UnauthorizedAccessException("Only admins can delete leads");
//        }
//        if (!leadRepository.existsById(id)) {
//            throw new ResourceNotFoundException("Lead not found with ID: " + id);
//        }
//        leadRepository.deleteById(id);
//    }

    @Override
    @Transactional
    public void deleteLead(Long id, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can delete leads");
        }
        if (!leadRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lead not found with ID: " + id);
        }


// SAFE DELETE: dissociate deals that reference this lead by setting lead to null
        try {
            List<Deal> deals = dealRepository.findByLeadId(id);
            if (deals != null && !deals.isEmpty()) {
                for (Deal d : deals) {
                    d.setLead(null); // clear association
//                    d.setLeadId(null); // if using primitive field leadId as well
                    dealRepository.save(d);
                }
                log.info("Dissociated {} deals from lead {} before deleting lead", deals.size(), id);
            }
        } catch (Exception e) {
            log.warn("Failed to dissociate deals for lead {}: {}", id, e.getMessage());
// proceed with deletion anyway or rethrow — choose to proceed after logging
        }


// Now delete lead
        leadRepository.deleteById(id);
        log.info("Deleted lead with id {}", id);
    }

    // ---------------- CSV import enhancements for Lead ----------------

    @Override
    @Transactional
    public List<ImportResult> importLeadsFromCsv(MultipartFile file, String authHeader) {
        List<ImportResult> results = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            results.add(new ImportResult(0, "ERROR", "Empty or missing file", null));
            return results;
        }

        String token = extractToken(authHeader);
        String currentEmployeeId = jwtUtil.extractSubject(token);
        boolean isAdmin = jwtUtil.isAdmin(token);

        // pre-load existing leads for duplicate mobile checks (ok for moderate datasets)
        List<Lead> existingLeads = leadRepository.findAll();

        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim(false)
                    .withAllowMissingColumnNames() // important to ignore trailing empty headers
                    .parse(reader);

            // build normalized header map (normalized -> actual header name)
            Map<String, String> headerMap = new HashMap<>();
            for (String raw : parser.getHeaderMap().keySet()) {
                if (raw == null) continue;
                String nk = normalizeHeaderKey(raw);
                if (nk.isBlank()) {
                    log.debug("Ignoring blank header column (raw='{}')", raw);
                    continue;
                }
                if (headerMap.containsKey(nk)) {
                    log.warn("Duplicate normalized CSV header '{}' found (keeping first: '{}' ; ignoring: '{}')", nk, headerMap.get(nk), raw);
                    continue;
                }
                headerMap.put(nk, raw);
            }

            int rowCounter = 0;
            for (CSVRecord record : parser) {
                rowCounter = (int) record.getRecordNumber() + 1;
                try {
                    LeadImport imp = mapRecordToLeadImport(record, headerMap);

                    // normalize fields
                    String name = safeTrim(imp.getName());
                    String emailRaw = safeTrim(imp.getEmail());
                    String email = emailRaw == null ? null : emailRaw.toLowerCase();
                    String mobileRaw = safeTrim(imp.getMobileNumber());
                    String mobileNormalized = normalizeMobileDigits(mobileRaw);

                    // skip empty rows
                    if ((name == null || name.isBlank()) && (email == null || email.isBlank()) && (mobileRaw == null || mobileRaw.isBlank())) {
                        results.add(new ImportResult(rowCounter, "SKIPPED", "Empty row (no name/email/mobile)", null));
                        continue;
                    }

                    // duplicate checks
                    boolean emailExists = false;
                    if (email != null && !email.isBlank()) {
                        emailExists = leadRepository.existsByEmailIgnoreCase(email);
                    }

                    boolean mobileExists = false;
                    if (mobileNormalized != null && !mobileNormalized.isBlank()) {
                        for (Lead ex : existingLeads) {
                            String exMob = normalizeMobileDigits(ex.getMobileNumber());
                            if (exMob != null && exMob.equals(mobileNormalized)) {
                                mobileExists = true;
                                break;
                            }
                        }
                    }

                    if (emailExists) {
                        results.add(new ImportResult(rowCounter, "SKIPPED", "Duplicate email", null));
                        continue;
                    }
                    if (mobileExists) {
                        results.add(new ImportResult(rowCounter, "SKIPPED", "Duplicate mobile", null));
                        continue;
                    }

                    // create entity
                    Lead lead = new Lead();
                    lead.setName(name);
                    lead.setEmail(email);
                    lead.setCompanyName(safeTrim(imp.getCompanyName()));
                    lead.setOfficialWebsite(safeTrim(imp.getOfficialWebsite()));
                    lead.setMobileNumber(safeTrim(imp.getMobileNumber()));
                    lead.setOfficePhone(safeTrim(imp.getOfficePhone()));
                    lead.setCity(safeTrim(imp.getCity()));
                    lead.setState(safeTrim(imp.getState()));
                    lead.setPostalCode(safeTrim(imp.getPostalCode()));
                    lead.setCountry(safeTrim(imp.getCountry()));
                    lead.setCompanyAddress(safeTrim(imp.getCompanyAddress()));

                    if (!isAdmin) {
                        lead.setLeadOwner(currentEmployeeId);
                        lead.setAddedBy(currentEmployeeId);
                    } else {
                        lead.setAddedBy(currentEmployeeId);
                        lead.setLeadOwner(currentEmployeeId);
                    }

                    Lead saved = leadRepository.save(lead);
                    existingLeads.add(saved);
                    results.add(new ImportResult(rowCounter, "CREATED", null, saved.getId()));

                } catch (Exception e) {
                    log.error("Row import failed at {}: {}", rowCounter, safeMessage(e), e);
                    results.add(new ImportResult(rowCounter, "ERROR", "Parse or save error: " + safeMessage(e), null));
                }
            }

        } catch (Exception ex) {
            log.error("Failed to parse file: {}", safeMessage(ex), ex);
            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(ex), null));
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public LeadDealStatsDto getLeadDealStats(Long leadId, String authHeader) {
        // Ensure lead exists & access allowed
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + leadId));

        // Reuse existing access check (throws if not allowed)
        checkLeadAccess(lead, authHeader);

        Long total = 0L;
        Long wins = 0L;
        try {
            total = dealRepository.countByLeadId(leadId);
            wins = dealRepository.countWinsByLeadId(leadId);
        } catch (Exception ex) {
            log.warn("Failed to get counts from repository for lead {}: {} - falling back to list scan", leadId, ex.getMessage());
            // fallback: compute via list
            List<Deal> deals = dealRepository.findByLeadId(leadId);
            total = (long) deals.size();
            wins = deals.stream()
                    .filter(d -> d.getDealStage() != null && "win".equalsIgnoreCase(d.getDealStage()))
                    .count();
        }

        return LeadDealStatsDto.builder()
                .leadId(leadId)
                .totalDeals(total == null ? 0L : total)
                .convertedDeals(wins == null ? 0L : wins)
                .build();
    }


    // ---------------- helper / mapping functions ----------------

    private static String normalizeHeaderKey(String header) {
        if (header == null) return "";
        return header.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private LeadImport mapRecordToLeadImport(CSVRecord record, Map<String, String> headerMapping) {
        LeadImport imp = new LeadImport();

        java.util.function.Function<String[], String> valueOf = (keys) -> {
            for (String k : keys) {
                String nk = normalizeHeaderKey(k);
                String actual = headerMapping.get(nk);
                if (actual != null && record.isMapped(actual)) {
                    String v = record.get(actual);
                    if (v != null) return v.trim();
                }
            }
            return null;
        };

        imp.setName(valueOf.apply(new String[]{"name","full name","contact name"}));
        imp.setEmail(valueOf.apply(new String[]{"email","e-mail","email address"}));
        imp.setCompanyName(valueOf.apply(new String[]{"company","company name","organization"}));
        imp.setOfficialWebsite(valueOf.apply(new String[]{"website","official website","site","url"}));
        imp.setMobileNumber(valueOf.apply(new String[]{"mobile","mobile number","phone","phone number","mobile_no","mobile_no."}));
        imp.setOfficePhone(valueOf.apply(new String[]{"officephone","office phone","office_phone","phone_office","landline"}));
        imp.setCity(valueOf.apply(new String[]{"city"}));
        imp.setState(valueOf.apply(new String[]{"state","region","province"}));
        imp.setPostalCode(valueOf.apply(new String[]{"postalcode","postal code","zip","zip code"}));
        imp.setCountry(valueOf.apply(new String[]{"country"}));
        imp.setCompanyAddress(valueOf.apply(new String[]{"address","company address","office address","company_address"}));
        // optional date column
        return imp;
    }

    private static String safeTrim(String s) { return s == null ? null : s.trim(); }

    private static String normalizeMobileDigits(String mobile) {
        if (mobile == null) return null;
        String digits = mobile.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    private static String safeMessage(Exception e) { return e == null ? "" : (e.getMessage() == null ? e.toString() : e.getMessage()); }

    // Flexible datetime parser — supports many common formats (date-only or date+time)
    private LocalDateTime parseFlexibleDateTime(String input) {
        if (input == null || input.isBlank()) return null;

        List<String> patterns = List.of(
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "d MMM yyyy",
                "dd MMM yyyy",
                "yyyy/MM/dd"
        );

        for (String p : patterns) {
            try {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern(p);
                if (p.contains("H") || p.contains("T")) {
                    return LocalDateTime.parse(input, fmt);
                } else {
                    LocalDate d = LocalDate.parse(input, fmt);
                    return d.atStartOfDay();
                }
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    // ---------------- existing helper methods (unchanged) ----------------
    private void validateEmployeeExists(String employeeId, String authHeader) {
        try {
            Boolean exists = employeeClient.checkEmployeeExists(employeeId, authHeader);
            if (exists == null || !exists) {
                throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
            }
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Employee not found with ID: " + employeeId);
        } catch (FeignException e) {
            log.error("Feign error validating employee {}: status={}, body={}", employeeId, e.status(), safeContent(e));
            throw new RuntimeException("Error validating employee: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error validating employee {}: {}", employeeId, e.getMessage());
            throw new RuntimeException("Unexpected error validating employee");
        }
    }

    private String safeContent(FeignException e) {
        try { return e.contentUTF8(); } catch (Exception ex) { return "<no content>"; }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }

    private void autoConvertLeadToClient(Long leadId, String authHeader) {
        try {
            Lead lead = leadRepository.findById(leadId).orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
            List<ClientResponseDto> existing = null;
            try {
                existing = clientClient.getClientsByEmail(lead.getEmail(), authHeader);
            } catch (FeignException fe) {
                log.warn("Feign error checking clients by email for lead {}: {}", leadId, safeContent(fe));
            }

            if (existing != null && !existing.isEmpty()) {
                log.info("Client already exists with email {}, skipping conversion", lead.getEmail());
                try { lead.setStatus(CONVERTED); } catch (Exception ex) { lead.setStatus(CONVERTED); }
                leadRepository.save(lead);
                return;
            }

            ClientRequestDto clientDto = new ClientRequestDto();
            clientDto.setName(lead.getName());
            clientDto.setEmail(lead.getEmail());
            clientDto.setMobile(lead.getMobileNumber());
            clientDto.setCountry(lead.getCountry());
            clientDto.setCategory(lead.getClientCategory());
            clientDto.setCompanyName(lead.getCompanyName());
            clientDto.setWebsite(lead.getOfficialWebsite());

            String clientJson;
            try {
                clientJson = objectMapper.writeValueAsString(clientDto);
            } catch (Exception e) {
                log.error("Failed to serialize client DTO for lead {}: {}", leadId, e.getMessage(), e);
                return;
            }

            try {
                ClientResponseDto created = clientClient.createClient(clientJson, null, null, authHeader);
                if (created != null) {
                    try { lead.setStatus(LeadStatus.CONVERTED); } catch (Exception ex) { lead.setStatus(LeadStatus.CONVERTED); }
                    leadRepository.save(lead);
                    log.info("Lead {} converted to client {}", leadId, created.getId());
                } else {
                    log.warn("Client service returned null while creating client for lead {}", leadId);
                }
            } catch (FeignException fe) {
                log.error("Feign error converting lead to client: status={}, body={}", fe.status(), safeContent(fe));
            } catch (Exception e) {
                log.error("Error converting lead to client: {}", e.getMessage(), e);
            }

        } catch (ResourceNotFoundException rnfe) {
            log.warn("Lead not found during auto-convert: {}", leadId);
        } catch (Exception e) {
            log.error("Unexpected error in autoConvertLeadToClient for lead {}: {}", leadId, e.getMessage(), e);
        }
    }

    private EmployeeMetaDto buildEmployeeMeta(String employeeId, String authHeader) {
        if (employeeId == null || employeeId.isBlank()) return null;
        try {
            var emp = employeeClient.getEmployeeById(employeeId, authHeader);
            return toMeta(emp);
        } catch (feign.FeignException.Forbidden | feign.FeignException.Unauthorized ex) {
            try {
                var meta = employeeClient.getMeta(employeeId);
                return meta;
            } catch (Exception e2) {
                log.warn("Fallback meta fetch failed for {}: {}", employeeId, e2.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.warn("Meta fetch failed for {}: {}", employeeId, e.getMessage());
            return null;
        }
    }

    private EmployeeMetaDto toMeta(EmployeeDto emp) {
        if (emp == null) return null;
        EmployeeMetaDto m = new EmployeeMetaDto();
        m.setEmployeeId(emp.getEmployeeId());
        m.setName(emp.getName());
        m.setDesignation(emp.getDesignationName());
        m.setDepartment(emp.getDepartmentName());
        m.setProfileUrl(emp.getProfilePictureUrl());
        return m;
    }

}
