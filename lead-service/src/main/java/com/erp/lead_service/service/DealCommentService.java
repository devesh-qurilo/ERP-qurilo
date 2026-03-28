package com.erp.lead_service.service;

import com.erp.lead_service.dto.CommentRequestDto;
import com.erp.lead_service.dto.CommentResponseDto;

import java.util.List;

public interface DealCommentService {
    CommentResponseDto addComment(Long dealId, CommentRequestDto dto, String authHeader);
    List<CommentResponseDto> getComments(Long dealId, String authHeader);
    CommentResponseDto updateComment(Long dealId, Long commentId, CommentRequestDto dto, String authHeader);
    void deleteComment(Long dealId, Long commentId, String authHeader);
}
