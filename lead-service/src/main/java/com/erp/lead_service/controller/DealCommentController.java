package com.erp.lead_service.controller;

import com.erp.lead_service.dto.CommentRequestDto;
import com.erp.lead_service.dto.CommentResponseDto;
import com.erp.lead_service.service.DealCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deals/{dealId}/comments")
@RequiredArgsConstructor
public class DealCommentController {

    private final DealCommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> list(@PathVariable Long dealId,
                                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(commentService.getComments(dealId, auth));
    }

    @PostMapping
    public ResponseEntity<CommentResponseDto> add(@PathVariable Long dealId,
                                                  @Valid @RequestBody CommentRequestDto dto,
                                                  @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(commentService.addComment(dealId, dto, auth));
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDto> update(@PathVariable Long dealId,
                                                     @PathVariable Long commentId,
                                                     @Valid @RequestBody CommentRequestDto dto,
                                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(commentService.updateComment(dealId, commentId, dto, auth));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long dealId,
                                       @PathVariable Long commentId,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        commentService.deleteComment(dealId, commentId, auth);
        return ResponseEntity.noContent().build();
    }
}
