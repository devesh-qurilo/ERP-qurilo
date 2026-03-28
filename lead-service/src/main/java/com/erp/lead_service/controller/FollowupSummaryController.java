package com.erp.lead_service.controller;

import com.erp.lead_service.dto.FollowupSummaryDto;
import com.erp.lead_service.service.FollowupSummaryService;
import com.erp.lead_service.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deals/followups")
public class FollowupSummaryController {

    private final FollowupSummaryService summaryService;
    private final JwtUtil jwtUtil;

    public FollowupSummaryController(FollowupSummaryService summaryService, JwtUtil jwtUtil) {
        this.summaryService = summaryService;
        this.jwtUtil = jwtUtil;
    }

    // admin-only: you can adapt to allow non-admins if needed
    @GetMapping("/summary")
    public ResponseEntity<FollowupSummaryDto> summary(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = (authHeader == null) ? null : (authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader);
        if (!jwtUtil.isAdmin(token)) {
            return ResponseEntity.status(403).build();
        }
        FollowupSummaryDto dto = summaryService.getGlobalFollowupSummary();
        return ResponseEntity.ok(dto);
    }
}
