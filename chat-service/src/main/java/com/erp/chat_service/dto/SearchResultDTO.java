package com.erp.chat_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResultDTO {
    private List<EmployeeDTO> employees;
    private List<ChatMessageResponse> messages;
}
