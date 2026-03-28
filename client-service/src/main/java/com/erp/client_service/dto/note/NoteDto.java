package com.erp.client_service.dto.note;

import com.erp.client_service.entity.NoteType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteDto {
    private Long id;
    private Long clientId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Detail is required")
    private String detail;

    private NoteType type; // PUBLIC or PRIVATE
    private String createdBy;
    private String createdAt;
}
