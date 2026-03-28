package com.erp.chat_service.controller;

import com.erp.chat_service.dto.SearchResultDTO;
import com.erp.chat_service.service.ChatSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/search")
public class ChatSearchController {

    @Autowired
    private ChatSearchService chatSearchService;

    @GetMapping
    public SearchResultDTO search(
            @RequestParam String query,
            @RequestHeader("X-User-Id") String currentUserId) {
        return chatSearchService.searchChats(query, currentUserId);
    }
}
