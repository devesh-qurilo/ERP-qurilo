package com.erp.lead_service.service;

import com.erp.lead_service.client.EmployeeServiceClient;
import com.erp.lead_service.client.ClientServiceClient;
import com.erp.lead_service.dto.*;
import com.erp.lead_service.dto.Import.DealImport;
import com.erp.lead_service.dto.Import.ImportResult;
import com.erp.lead_service.dto.deal.DealRequestDto;
import com.erp.lead_service.dto.deal.DealFollowupDto;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import com.erp.lead_service.dto.deal.DealResponseDto;
import com.erp.lead_service.dto.deal.DealStatsDto;
import com.erp.lead_service.entity.*;
import com.erp.lead_service.event.LeadAutoConvertEvent;
import com.erp.lead_service.exception.ResourceNotFoundException;
import com.erp.lead_service.exception.UnauthorizedAccessException;
import com.erp.lead_service.mapper.DealMapper;
import com.erp.lead_service.repository.*;
import com.erp.lead_service.util.JwtUtil;
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

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepository;
    private final LeadRepository leadRepository;
    private final DealMapper dealMapper;
    private final EmployeeServiceClient employeeClient;
    private final JwtUtil jwtUtil;
    private final ClientServiceClient clientClient;
    private final PriorityRepository priorityRepository;


    private final DealTagRepository dealTagRepository;
    private final DealCommentRepository dealCommentRepository;
    private final DealEmployeeRepository dealEmployeeRepository;
    private final DealFollowUpRepository dealFollowupRepository;
    private final DealDocumentRepository dealDocumentRepository;
    private final EmployeeServiceClient employeeServiceClient;
    private final DealNoteRepository dealNoteRepository;

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public DealResponseDto createDeal(DealRequestDto dto, String authHeader) {
        String token = extractToken(authHeader);
        String currentEmployee = jwtUtil.extractSubject(token);
        boolean isAdmin = jwtUtil.isAdmin(token);

        // Validate agent exists (if provided)
        if (dto.getDealAgent() != null) {
            validateEmployeeExists(dto.getDealAgent(), authHeader);
        }

        // Validate watchers (if any)
        if (dto.getDealWatchers() != null && !dto.getDealWatchers().isEmpty()) {
            for (String watcher : dto.getDealWatchers()) {
                validateEmployeeExists(watcher, authHeader);
            }
        }

        // Resolve lead if provided
        Lead lead = null;
        if (dto.getLeadId() != null) {
            lead = leadRepository.findById(dto.getLeadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + dto.getLeadId()));
        }

        // Map to entity and set defaults
        Deal entity = dealMapper.toEntity(dto, lead);
        try { entity.setCreatedAt(java.time.LocalDateTime.now()); } catch (Exception ignored) {}

        // Persist
        Deal saved = dealRepository.save(entity);
        log.info("Deal created with id {}", saved.getId());

        // If the deal stage is WIN and lead exists and lead.autoConvertToClient == true
        if ("WIN".equalsIgnoreCase(saved.getDealStage()) && saved.getLead() != null
                && Boolean.TRUE.equals(saved.getLead().getAutoConvertToClient())) {
            // publish event for AFTER_COMMIT processing (so lead changes commit first)
            try {
                // IMPORTANT: pass the full authHeader (including "Bearer ...") so client-service can authorize
                eventPublisher.publishEvent(new LeadAutoConvertEvent(saved.getLead().getId(), authHeader));
                log.info("Published LeadAutoConvertEvent (from createDeal) for lead {}", saved.getLead().getId());
            } catch (Exception e) {
                log.warn("Failed to publish LeadAutoConvertEvent from createDeal for lead {}: {}", saved.getLead().getId(), e.getMessage());
            }
        }

        return dealMapper.toDto(saved);
    }



    @Override
    public DealResponseDto getDealById(Long id, String authHeader) {
        Deal deal = dealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found"));

        return enrichDealResponseDto(deal, authHeader);
    }

    // Common enrichment method
    private DealResponseDto enrichDealResponseDto(Deal deal, String authHeader) {
        DealResponseDto dto = dealMapper.toDto(deal);
        Long id = deal.getId();

        // enrich
        dto.setTags(loadTags(id));
        dto.setComments(loadComments(id));
        dto.setAssignedEmployeesMeta(loadAssignedEmployeesMeta(id, deal, authHeader));
        dto.setFollowups(loadFollowups(id));

        // agent & watchers meta
        dto.setDealAgentMeta(buildEmployeeMeta(deal.getDealAgent(), authHeader));
        dto.setDealWatchersMeta(buildEmployeesMeta(deal.getDealWatchers(), authHeader));
        dto.setPriority(loadPriorityForDeal(id));


        return dto;
    }
    private PriorityDto loadPriorityForDeal(Long dealId) {
        try {
            List<Priority> priorities = priorityRepository.findByDealId(dealId);
            if (priorities == null || priorities.isEmpty()) return null;
            Priority p = priorities.get(0); // service currently stores only one per deal
            PriorityDto d = new PriorityDto();
            d.setId(p.getId());
            d.setStatus(p.getStatus());
            d.setColor(p.getColor());
            d.setDealId(p.getDeal() != null ? p.getDeal().getId() : null);
            d.setIsGlobal(Boolean.TRUE.equals(p.getIsGlobal()));
            return d;
        } catch (Exception e) {
            log.warn("Unable to load priority for deal {}: {}", dealId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<DealResponseDto> getAllDeals(String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can access all deals");
        }

        return dealRepository.findAll().stream().map(deal -> {
            DealResponseDto dto = dealMapper.toDto(deal);
            Long id = deal.getId();

            // enrich (same as getById)
            dto.setTags(loadTags(id));
            dto.setComments(loadComments(id));
            dto.setAssignedEmployeesMeta(loadAssignedEmployeesMeta(id, deal, authHeader));
            dto.setFollowups(loadFollowups(id));

            dto.setDealAgentMeta(buildEmployeeMeta(deal.getDealAgent(), authHeader));
            dto.setDealWatchersMeta(buildEmployeesMeta(deal.getDealWatchers(), authHeader));
            // NEW: attach priority so GET /deals returns priority for each deal
            dto.setPriority(loadPriorityForDeal(id));

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DealResponseDto updateDealStage(Long dealId, String newStage, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) throw new UnauthorizedAccessException("Only admins can update deal stage");

        Deal deal = dealRepository.findById(dealId).orElseThrow(() -> new ResourceNotFoundException("Deal not found"));
        String oldStage = deal.getDealStage();
        deal.setDealStage(newStage);
        Deal updated = dealRepository.save(deal);

        if ("WIN".equalsIgnoreCase(newStage) && updated.getLead() != null
                && Boolean.TRUE.equals(updated.getLead().getAutoConvertToClient())) {
            try {
                eventPublisher.publishEvent(new LeadAutoConvertEvent(updated.getLead().getId(), authHeader));
                log.info("Published LeadAutoConvertEvent (from updateDealStage) for lead {}", updated.getLead().getId());
            } catch (Exception e) {
                log.warn("Failed to publish LeadAutoConvertEvent from updateDealStage for lead {}: {}", updated.getLead().getId(), e.getMessage());
            }
        }

        log.info("Deal stage updated from {} to {} for deal ID: {}", oldStage, newStage, dealId);
        return dealMapper.toDto(updated);
    }

    @Transactional
    public DealResponseDto updateDeal(Long dealId, DealRequestDto dto, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can update deals");
        }

        Deal deal = dealRepository.findById(dealId).orElseThrow(() -> new ResourceNotFoundException("Deal not found"));

        if (dto.getTitle() != null) deal.setTitle(dto.getTitle());
        if (dto.getPipeline() != null) deal.setPipeline(dto.getPipeline());
        if (dto.getDealStage() != null) deal.setDealStage(dto.getDealStage());
        if (dto.getDealCategory() != null) deal.setDealCategory(dto.getDealCategory());
        if (dto.getValue() != null) deal.setValue(dto.getValue());
        if (dto.getExpectedCloseDate() != null) deal.setExpectedCloseDate(dto.getExpectedCloseDate());
        if (dto.getDealAgent() != null) {
            validateEmployeeExists(dto.getDealAgent(), authHeader);
            deal.setDealAgent(dto.getDealAgent());
        }
        if (dto.getDealWatchers() != null) {
            for (String w : dto.getDealWatchers()) validateEmployeeExists(w, authHeader);
            deal.setDealWatchers(dto.getDealWatchers());
        }

        Deal updated = dealRepository.save(deal);

        if ("WIN".equalsIgnoreCase(updated.getDealStage()) && updated.getLead() != null
                && Boolean.TRUE.equals(updated.getLead().getAutoConvertToClient())) {
            try {
                eventPublisher.publishEvent(new LeadAutoConvertEvent(updated.getLead().getId(), authHeader));
                log.info("Published LeadAutoConvertEvent (from updateDeal) for lead {}", updated.getLead().getId());
            } catch (Exception e) {
                log.warn("Failed to publish LeadAutoConvertEvent from updateDeal for lead {}: {}", updated.getLead().getId(), e.getMessage());
            }
        }

        return dealMapper.toDto(updated);
    }

    @Override
    public List<DealResponseDto> getDealByLeadId(Long id, String auth) {
        if (!jwtUtil.isAdmin(extractToken(auth))) {
            throw new UnauthorizedAccessException("Only admins can access all deals");
        }

        return dealRepository.findByLeadId(id).stream().map(deal -> {
            DealResponseDto dto = dealMapper.toDto(deal);
            Long id1 = deal.getId();

            // enrich (same as getById)
            dto.setTags(loadTags(id1));
            dto.setComments(loadComments(id1));
            dto.setAssignedEmployeesMeta(loadAssignedEmployeesMeta(id1, deal, auth));
            dto.setFollowups(loadFollowups(id1));

            dto.setDealAgentMeta(buildEmployeeMeta(deal.getDealAgent(), auth));
            dto.setDealWatchersMeta(buildEmployeesMeta(deal.getDealWatchers(), auth));
            // NEW: attach priority so GET /deals returns priority for each deal
            dto.setPriority(loadPriorityForDeal(id));

            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public DealResponseDto applyBulkOperations(Long dealId, BulkDealOpsDto dto, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can add it into deals");
        }
        Deal deal = dealRepository.findById(dealId).orElseThrow(() -> new ResourceNotFoundException("Deal not found"));

        if (dto.getTags() != null) {
            for (String t : dto.getTags()) {
                DealTag tag = new DealTag();
                tag.setDeal(deal);
                tag.setTagName(t);
                dealTagRepository.save(tag);
            }
        }

        if (dto.getEmployeeIds() != null) {
            for (String emp : dto.getEmployeeIds()) {
                if (!dealEmployeeRepository.existsByDealIdAndEmployeeId(dealId, emp)) {
                    DealEmployee de = new DealEmployee();
                    de.setDeal(deal);
                    de.setEmployeeId(emp);
                    dealEmployeeRepository.save(de);
                }
            }
        }

        if (dto.getComments() != null) {
            String token = extractToken(authHeader);
            String current = jwtUtil.extractSubject(token);
            for (CommentRequestDto c : dto.getComments()) {
                DealComment dc = new DealComment();
                dc.setDeal(deal);
                dc.setCommentText(c.getCommentText());
                dc.setEmployeeId(current);
                dealCommentRepository.save(dc);
            }
        }

        return getDealById(dealId, authHeader);
    }

    private void validateEmployeeExists(String employeeId, String authHeader) {
        try {
            Boolean exists = employeeClient.checkEmployeeExists(employeeId, authHeader);
            if (exists == null || !exists) throw new ResourceNotFoundException("Employee not found: " + employeeId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        } catch (FeignException e) {
            log.error("Feign error validating employee {}: status={}, body={}", employeeId, e.status(), safeContent(e));
            throw new RuntimeException("Error validating employee: " + e.getMessage());
        }
    }

    private String safeContent(FeignException e) {
        try { return e.contentUTF8(); } catch (Exception ex) { return "<no content>"; }
    }

    private String extractToken(String authHeader) {
        if (authHeader == null) return null;
        if (authHeader.startsWith("Bearer ")) return authHeader.substring(7);
        return authHeader;
    }

    // ---------- Helpers (NEW) ----------
    private EmployeeMetaDto buildEmployeeMeta(String employeeId, String authHeader) {
        if (employeeId == null) return null;
        try {
            var emp = employeeClient.getEmployeeById(employeeId, authHeader);
            if (emp == null) return null;
            EmployeeMetaDto em = new EmployeeMetaDto();
            em.setEmployeeId(emp.getEmployeeId());
            em.setName(emp.getName());
            em.setDesignation(emp.getDesignationName());
            em.setDepartment(emp.getDepartmentName());
            em.setProfileUrl(emp.getProfilePictureUrl());
            return em;
        } catch (Exception e) {
            log.warn("Unable to fetch employee {} metadata: {}", employeeId, e.getMessage());
            return null;
        }
    }
    private List<EmployeeMetaDto> buildEmployeesMeta(List<String> employeeIds, String authHeader) {
        if (employeeIds == null || employeeIds.isEmpty()) return List.of();
        List<EmployeeMetaDto> out = new ArrayList<>();
        for (String id : employeeIds) {
            EmployeeMetaDto m = buildEmployeeMeta(id, authHeader);
            if (m != null) out.add(m);
        }
        return out;
    }
    private List<String> loadTags(Long dealId) {
        return dealTagRepository.findByDealId(dealId)
                .stream().map(DealTag::getTagName).collect(Collectors.toList());
    }
    private List<CommentResponseDto> loadComments(Long dealId) {
        return dealCommentRepository.findByDealIdOrderByCreatedAtDesc(dealId)
                .stream().map(c -> {
                    CommentResponseDto cr = new CommentResponseDto();
                    cr.setId(c.getId());
                    cr.setEmployeeId(c.getEmployeeId());
                    cr.setCommentText(c.getCommentText());
                    cr.setCreatedAt(c.getCreatedAt());
                    return cr;
                }).collect(Collectors.toList());
    }
    private List<EmployeeMetaDto> loadAssignedEmployeesMeta(Long dealId, Deal deal, String authHeader) {
        Set<String> allEmpIds = new HashSet<>();
        if (deal.getDealWatchers() != null) allEmpIds.addAll(deal.getDealWatchers());
        allEmpIds.addAll(
                dealEmployeeRepository.findByDealId(dealId)
                        .stream().map(DealEmployee::getEmployeeId)
                        .collect(Collectors.toSet())
        );
        List<EmployeeMetaDto> meta = new ArrayList<>();
        for (String eid : allEmpIds) {
            EmployeeMetaDto m = buildEmployeeMeta(eid, authHeader);
            if (m != null) meta.add(m);
        }
        return meta;
    }
    private List<DealFollowupDto> loadFollowups(Long dealId) {
        return dealFollowupRepository.findByDealIdOrderByNextDateAsc(dealId)
                .stream()
                .map(f -> {
                    DealFollowupDto d = new DealFollowupDto();
                    d.setId(f.getId());
                    d.setId(f.getDeal().getId());
                    d.setNextDate(f.getNextDate());
                    d.setStartTime(LocalTime.parse(f.getStartTime()));
                    d.setRemarks(f.getRemarks());
                    d.setSendReminder(Boolean.TRUE.equals(f.getSendReminder()));
                    d.setReminderSent(Boolean.TRUE.equals(f.getReminderScheduled()));
                    d.setCreatedAt(f.getCreatedAt());
                    return d;
                }).collect(Collectors.toList());
    }

    //Importing deals

    @Override
    @Transactional
    public List<ImportResult> importDealsFromCsv(MultipartFile file, String authHeader) {
        List<ImportResult> results = new ArrayList<>();
        if (file == null || file.isEmpty()) return results;

        String token = extractToken(authHeader);
        // we will reuse createDeal(...) so permissions/employee validation remains consistent

        try (InputStream in = file.getInputStream();
             var reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {

            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim(false) // we'll trim per-value
                    .parse(reader);

            // Build normalized header map: normalizedKey -> actual header name in CSV
            Map<String, String> headerMap = new HashMap<>();
            for (String h : parser.getHeaderMap().keySet()) {
                if (h == null) continue;
                headerMap.put(normalizeHeaderKey(h), h);
            }

            // iterate records
            for (CSVRecord record : parser) {
                int rowNum = (int) record.getRecordNumber() + 1; // header + data rows counting
                try {
                    DealImport imp = mapRecordToDealImport(record, headerMap);

                    // normalize and validate
                    String title = safeTrim(imp.getTitle());
                    if (title == null || title.isBlank()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing title", null));
                        continue;
                    }

                    // parse value (optional)
                    Double value = null;
                    if (imp.getValue() != null && !imp.getValue().isBlank()) {
                        try {
                            String v = imp.getValue().replaceAll("[^0-9.\\-]", "");
                            if (!v.isBlank()) value = Double.parseDouble(v);
                        } catch (NumberFormatException nfe) {
                            results.add(new ImportResult(rowNum, "ERROR", "Invalid value: " + imp.getValue(), null));
                            continue;
                        }
                    }

                    // parse expectedCloseDate (optional) - try ISO then d/M/yyyy
                    LocalDate expected = null;
                    if (imp.getExpectedCloseDate() != null && !imp.getExpectedCloseDate().isBlank()) {
                        String d = imp.getExpectedCloseDate().trim();
                        try {
                            expected = LocalDate.parse(d, DateTimeFormatter.ISO_LOCAL_DATE);
                        } catch (Exception e1) {
                            try {
                                expected = LocalDate.parse(d, DateTimeFormatter.ofPattern("d/M/yyyy"));
                            } catch (Exception e2) {
                                results.add(new ImportResult(rowNum, "ERROR", "Invalid date: " + d, null));
                                continue;
                            }
                        }
                    }

                    // find lead by name (case-insensitive)
                    String leadName = safeTrim(imp.getLeadName());
                    if (leadName == null || leadName.isBlank()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Missing lead name", null));
                        continue;
                    }

                    var leadOpt = leadRepository.findByNameIgnoreCase(leadName);
                    if (leadOpt.isEmpty()) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Lead not found: " + leadName, null));
                        continue;
                    }
                    var lead = leadOpt.get();

                    // duplicate check: title (ignore-case) + lead.id
                    boolean dup = dealRepository.existsByTitleIgnoreCaseAndLeadId(title, lead.getId());
                    if (dup) {
                        results.add(new ImportResult(rowNum, "SKIPPED", "Duplicate deal (title + lead)", null));
                        continue;
                    }

                    // Build DealRequestDto and call existing createDeal
                    com.erp.lead_service.dto.deal.DealRequestDto dto = new com.erp.lead_service.dto.deal.DealRequestDto();
                    dto.setTitle(title);
                    dto.setValue(value);
                    dto.setDealStage(safeTrim(imp.getDealStage()));
                    dto.setPipeline(safeTrim(imp.getPipeline()));
                    dto.setExpectedCloseDate(expected);
                    dto.setLeadId(lead.getId());
                    // Note: createDeal will validate dealAgent/dealWatchers if provided. We don't set agent/watchers here.

                    com.erp.lead_service.dto.deal.DealResponseDto created = createDeal(dto, authHeader);
                    results.add(new ImportResult(rowNum, "CREATED", null, created.getId()));

                } catch (Exception e) {
                    results.add(new ImportResult(rowNum, "ERROR", "Unhandled: " + safeMessage(e), null));
                }
            }

        } catch (Exception ex) {
            results.add(new ImportResult(0, "ERROR", "Failed to parse file: " + safeMessage(ex), null));
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public DealStatsDto getGlobalDealStats(String authHeader) {
        // Restrict to admins (reuse jwt util)
        String token = extractToken(authHeader);
        if (!jwtUtil.isAdmin(token)) {
            throw new UnauthorizedAccessException("Only admins can access global deal stats");
        }

        Long total = 0L;
        Long wins = 0L;
        try {
            total = dealRepository.countAllDeals();
            wins = dealRepository.countAllWins();
        } catch (Exception ex) {
            log.warn("Repo count query failed, falling back to list scan: {}", ex.getMessage());
            List<Deal> all = dealRepository.findAll();
            total = (long) all.size();
            wins = all.stream().filter(d -> d.getDealStage() != null && "win".equalsIgnoreCase(d.getDealStage())).count();
        }

        return DealStatsDto.builder()
                .totalDeals(total == null ? 0L : total)
                .convertedDeals(wins == null ? 0L : wins)
                .build();
    }

    @Override
    @Transactional
    public void deleteDeal(Long id, String authHeader) {
        if (!jwtUtil.isAdmin(extractToken(authHeader))) {
            throw new UnauthorizedAccessException("Only admins can delete deals");
        }


        if (!dealRepository.existsById(id)) {
            throw new ResourceNotFoundException("Deal not found with ID: " + id);
        }


// Optionally cleanup child entities related to deal (comments, tags, followups, documents, employees)
        try {
            dealCommentRepository.deleteByDealId(id);
        } catch (Exception e) {
            log.warn("Failed to delete comments for deal {}: {}", id, e.getMessage());
        }
        try { dealTagRepository.deleteByDealId(id); } catch (Exception ignored) {}
        try { dealEmployeeRepository.deleteByDealId(id); } catch (Exception ignored) {}
        try { dealFollowupRepository.deleteByDealId(id); } catch (Exception ignored) {}
        try { dealDocumentRepository.deleteByDealId(id); } catch (Exception ignored) {}
        try { priorityRepository.deleteByDealId(id);} catch (Exception ignored) {}
        try { dealNoteRepository.deleteByDealId(id);} catch (Exception ignored) {}

        dealRepository.deleteById(id);
        log.info("Deleted deal with id {}", id);
    }


    /* ----------------- helper methods ----------------- */

    private static String normalizeHeaderKey(String header) {
        if (header == null) return "";
        return header.trim().toLowerCase().replaceAll("\\s+","");
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * Map CSVRecord into DealImport using headerMap with tolerant keys (case-insensitive, spaces ignored)
     */
    private DealImport mapRecordToDealImport(CSVRecord rec, Map<String, String> headerMap) {
        DealImport imp = new DealImport();

        java.util.function.Function<String[], String> val = (keys) -> {
            for (String k : keys) {
                String nk = normalizeHeaderKey(k);
                String actual = headerMap.get(nk);
                if (actual != null && rec.isMapped(actual)) {
                    String v = rec.get(actual);
                    if (v != null) return v.trim();
                }
            }
            return null;
        };

        imp.setTitle(val.apply(new String[]{"title","deal name","name"}));
        imp.setValue(val.apply(new String[]{"value","deal value","amount"}));
        imp.setDealStage(val.apply(new String[]{"dealstage","stage"}));
        imp.setLeadName(val.apply(new String[]{"leadname","lead name","lead","lead_name","lead_name"}));
        imp.setExpectedCloseDate(val.apply(new String[]{"expectedclosedate","expected close date","close date","expected_close_date"}));
        imp.setPipeline(val.apply(new String[]{"pipeline","pipelines"}));

        return imp;
    }

    private static String safeMessage(Exception e) {
        if (e == null) return "";
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }
}
