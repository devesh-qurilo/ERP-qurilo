package com.erp.lead_service.controller;

import com.erp.lead_service.dto.DealDocumentDto;
import com.erp.lead_service.service.DealDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/deals/{dealId}/documents")
@RequiredArgsConstructor
public class DealDocumentController {

    private final DealDocumentService docService;

    @GetMapping
    public ResponseEntity<List<DealDocumentDto>> list(@PathVariable Long dealId,
                                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(docService.listDocuments(dealId, auth));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<DealDocumentDto> upload(@PathVariable Long dealId,
                                                  @RequestPart("file") MultipartFile file,
                                                  @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(docService.uploadDocument(dealId, file, auth));
    }

    @GetMapping("/{docId}")
    public ResponseEntity<DealDocumentDto> get(@PathVariable Long dealId,
                                               @PathVariable Long docId,
                                               @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(docService.getDocument(dealId, docId, auth));
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> delete(@PathVariable Long dealId, @PathVariable Long docId,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        docService.deleteDocument(dealId, docId, auth);
        return ResponseEntity.noContent().build();
    }
}
