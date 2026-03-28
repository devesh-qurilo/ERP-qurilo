package com.erp.lead_service.dto.lead;

import com.erp.lead_service.dto.EmployeeMetaDto;
import com.erp.lead_service.dto.deal.DealResponseDto;
import com.erp.lead_service.entity.LeadNote;
import com.erp.lead_service.entity.LeadStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeadResponseDto {
    private Long id;
    private String name;
    private String email;
    private String clientCategory;
    private String leadSource;
    private String leadOwner;
    private String addedBy;
    // NEW: rich employee meta to return in responses
    private EmployeeMetaDto leadOwnerMeta;
    private EmployeeMetaDto addedByMeta;
    private Boolean createDeal;
    private Boolean autoConvertToClient;
    private String companyName;
    private String officialWebsite;
    private String mobileNumber;
    private String officePhone;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String companyAddress;
    private LeadStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LeadNote> notes = new ArrayList<>();
    private List<DealResponseDto> deals = new ArrayList<>();
}