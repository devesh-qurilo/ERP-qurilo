package com.erp.chat_service.service;

import com.erp.chat_service.client.EmployeeClient;
import com.erp.chat_service.dto.ChatMessageResponse;
import com.erp.chat_service.dto.EmployeeDTO;
import com.erp.chat_service.dto.SearchResultDTO;
import com.erp.chat_service.entity.ChatMessage;
import com.erp.chat_service.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatSearchService {

    private final EmployeeClient employeeClient;
    private final ChatMessageRepository chatMessageRepository;

    public ChatSearchService(EmployeeClient employeeClient,
                             ChatMessageRepository chatMessageRepository) {
        this.employeeClient = employeeClient;
        this.chatMessageRepository = chatMessageRepository;
    }

    public SearchResultDTO searchChats(String searchQuery, String currentUserId) {
        SearchResultDTO result = new SearchResultDTO();

        // 1. Search employees
        List<EmployeeDTO> employees = employeeClient.searchEmployees(searchQuery);
        result.setEmployees(employees);

        // 2. Search messages - use the new method
        List<ChatMessage> messages = chatMessageRepository.searchMessagesForUser(searchQuery, currentUserId);
        List<ChatMessageResponse> messageDTOs = messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        result.setMessages(messageDTOs);

        return result;
    }

    private ChatMessageResponse convertToDTO(ChatMessage message) {
        ChatMessageResponse dto = new ChatMessageResponse();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setSenderId(message.getSenderId());
        dto.setReceiverId(message.getReceiverId());
        dto.setChatRoomId(message.getChatRoomId());
        // Add other fields as needed
        return dto;
    }
}