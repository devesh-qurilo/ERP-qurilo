package com.erp.chat_service.dto;


import com.erp.chat_service.entity.MessageType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "Receiver ID is required")
    private String receiverId;

    private String content;

    private MultipartFile file;

    private MessageType messageType = MessageType.TEXT;
}