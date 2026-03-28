package com.erp.finance_servic.dto.invoice.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    /**
     * Project name: project-service may return this field as "name" or "projectName".
     * Use @JsonAlias to accept both.
     */
    @JsonAlias({ "name", "projectName" })
    private String projectName;

    /**
     * Project code: project-service may return this as "shortCode" or "projectCode".
     */
    @JsonAlias({ "shortCode", "projectCode" })
    private String projectCode;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    private BigDecimal budget;

    private String currency;
}
