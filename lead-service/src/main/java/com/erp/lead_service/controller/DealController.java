package com.erp.lead_service.controller;

import com.erp.lead_service.dto.Import.ImportResult;
import com.erp.lead_service.dto.deal.DealRequestDto;
import com.erp.lead_service.dto.deal.DealResponseDto;
import com.erp.lead_service.dto.deal.DealStatsDto;
import com.erp.lead_service.service.DealCsvImportService;
import com.erp.lead_service.service.DealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @PostMapping
    public ResponseEntity<DealResponseDto> createDeal(@RequestBody DealRequestDto dto,
                                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(dealService.createDeal(dto, auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DealResponseDto> getDeal(@PathVariable Long id,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(dealService.getDealById(id, auth));
    }

    @GetMapping("/lead/{leadid}")
    public ResponseEntity<List<DealResponseDto>> getDealByLeadId(@PathVariable Long leadid,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(dealService.getDealByLeadId(leadid, auth));
    }

    @GetMapping
    public ResponseEntity<List<DealResponseDto>> getAllDeals(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(dealService.getAllDeals(auth));
    }

    @PutMapping("/{dealId}/stage")
    public ResponseEntity<DealResponseDto> updateDealStage(@PathVariable Long dealId,
                                                           @RequestParam String stage,
                                                           @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(dealService.updateDealStage(dealId, stage, auth));
    }

    @PutMapping("/{dealId}")
    public ResponseEntity<DealResponseDto> updateDeal(@PathVariable Long dealId,@RequestBody DealRequestDto dto,
                                                      @RequestHeader(value = "Authorization", required = false) String auth){
        return ResponseEntity.ok(dealService.updateDeal(dealId, dto, auth));
    }

    @PostMapping("/{dealId}/bulk")
    public ResponseEntity<DealResponseDto> bulkApply(
            @PathVariable Long dealId,
            @RequestBody com.erp.lead_service.dto.BulkDealOpsDto dto,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        DealResponseDto res = ((com.erp.lead_service.service.DealServiceImpl) dealService).applyBulkOperations(dealId, dto, auth);
        return ResponseEntity.ok(res);
    }

    // inside DealController

    // add a new field (or constructor param if using Lombok @RequiredArgsConstructor)
    private final DealCsvImportService dealCsvImportService;

    // then add endpoint:
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ImportResult>> importDealsCsv(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        List<ImportResult> res = dealCsvImportService.importDealsFromCsv(file, auth);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/stats")
    public ResponseEntity<DealStatsDto> getGlobalDealStats(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(dealService.getGlobalDealStats(auth));
    }

    // NEW: delete endpoint for deals
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeal(@PathVariable Long id,
                                           @RequestHeader(value = "Authorization", required = false) String auth) {
        dealService.deleteDeal(id, auth);
        return ResponseEntity.noContent().build();
    }

}
