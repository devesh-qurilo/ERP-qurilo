package com.erp.lead_service.controller;

import com.erp.lead_service.dto.FollowupRequestDto;
import com.erp.lead_service.dto.FollowupResponseDto;
import com.erp.lead_service.dto.FollowupUpdateRequestDto;
import com.erp.lead_service.service.DealFollowupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/deals/{dealId}/followups")
@RequiredArgsConstructor
public class DealFollowupController {

    private final DealFollowupService service;

    @GetMapping
    public ResponseEntity<List<FollowupResponseDto>> list(@PathVariable Long dealId,
                                                          @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.listFollowups(dealId, auth));
    }

    @GetMapping("/{followupId}")
    public ResponseEntity<FollowupResponseDto> get(@PathVariable Long dealId,
                                                   @PathVariable Long followupId,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.getFollowup(dealId, followupId, auth));
    }

    @PostMapping
    public ResponseEntity<FollowupResponseDto> add(@PathVariable Long dealId,
                                                   @Valid @RequestBody FollowupRequestDto dto,
                                                   @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.addFollowup(dealId, dto, auth));
    }

    @PutMapping("/{followupId}")
    public ResponseEntity<FollowupResponseDto> update(@PathVariable Long dealId,
                                                      @PathVariable Long followupId,
                                                      @Valid @RequestBody FollowupUpdateRequestDto dto,
                                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(service.updateFollowup(dealId, followupId, dto, auth));
    }

    @DeleteMapping("/{followupId}")
    public ResponseEntity<Void> delete(@PathVariable Long dealId, @PathVariable Long followupId,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        service.deleteFollowup(dealId, followupId, auth);
        return ResponseEntity.noContent().build();
    }
}
