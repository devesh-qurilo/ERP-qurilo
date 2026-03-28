package com.erp.employee_service.dto.award;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
public class AwardRequestDto {

    @NotBlank(message = "Award title is required")
    private String title;

    private String summary;

    private MultipartFile iconFile;
}