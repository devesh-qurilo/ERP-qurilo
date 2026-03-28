package com.erp.lead_service.controller;

import com.erp.lead_service.dto.Import.ImportResult;
import com.erp.lead_service.dto.Import.LeadImport;
import com.erp.lead_service.dto.lead.LeadDealStatsDto;
import com.erp.lead_service.dto.lead.LeadRequestDto;
import com.erp.lead_service.dto.lead.LeadResponseDto;
import com.erp.lead_service.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    public ResponseEntity<LeadResponseDto> createLead(
            @Valid @RequestBody LeadRequestDto dto,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(leadService.createLead(dto, auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadResponseDto> getLead(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(leadService.getLeadById(id, auth));
    }

    @GetMapping
    public ResponseEntity<List<LeadResponseDto>> getAllLeads(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(leadService.getAllLeads(auth));
    }

    @GetMapping("/my-leads")
    public ResponseEntity<List<LeadResponseDto>> getMyLeads(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(leadService.getMyLeads(auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadResponseDto> updateLead(
            @PathVariable Long id,
            @RequestBody LeadRequestDto dto,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(leadService.updateLead(id, dto, auth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLead(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        leadService.deleteLead(id, auth);
        return ResponseEntity.noContent().build();
    }

    // inside LeadController
    @PostMapping(value = "/import/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImportResult>> importLeadsFile(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        List<ImportResult> results = leadService.importLeadsFromCsv(file, auth);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}/deal-stats")
    public ResponseEntity<LeadDealStatsDto> getLeadDealStats(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        LeadDealStatsDto stats = leadService.getLeadDealStats(id, auth);
        return ResponseEntity.ok(stats);
    }

}
