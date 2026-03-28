package com.erp.project_service.dto.Discussion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionRoomRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    private Long categoryId;

    @NotBlank(message = "Initial message is required")
    @Size(min = 1, message = "Initial message cannot be empty")
    private String initialMessage;

    private MultipartFile initialFile;
}