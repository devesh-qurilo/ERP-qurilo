package com.erp.lead_service.controller;

import com.erp.lead_service.dto.TagRequestDto;
import com.erp.lead_service.dto.TagResponseDTO;
import com.erp.lead_service.entity.DealTag;
import com.erp.lead_service.service.DealTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deals/{dealId}/tags")
@RequiredArgsConstructor
public class DealTagController {

    private final DealTagService tagService;

    @GetMapping
    public ResponseEntity<List<TagResponseDTO>> getTags(@PathVariable Long dealId,
                                                        @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(tagService.getTagsForDeal(dealId, auth));
    }

    @PostMapping
    public ResponseEntity<String> addTag(@PathVariable Long dealId, @Valid @RequestBody TagRequestDto dto,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        tagService.addTag(dealId, dto, auth);
        return ResponseEntity.ok("Success");
    }

    // <-- fixed: include tagId in path
    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long dealId,
                                          @PathVariable Long tagId,
                                          @RequestHeader(value = "Authorization", required = false) String auth) {
        tagService.removeTag(dealId, tagId, auth);
        return ResponseEntity.noContent().build();
    }
}
