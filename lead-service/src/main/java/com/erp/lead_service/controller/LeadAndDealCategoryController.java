package com.erp.lead_service.controller;

import com.erp.lead_service.entity.DealCategory;
import com.erp.lead_service.entity.LeadSource;
import com.erp.lead_service.repository.DealcCategoryRepository;
import com.erp.lead_service.repository.LeadSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deals/dealCategory")
@RequiredArgsConstructor
public class LeadAndDealCategoryController {
    private final DealcCategoryRepository dealcCategoryRepository;
    private final LeadSourceRepository leadSourceRepository;

    //Adding Deal Category
    @PostMapping
    public ResponseEntity<DealCategory> saveDealCategory(@RequestBody DealCategory dealCategory) {
        return ResponseEntity.ok(dealcCategoryRepository.save(dealCategory));
    }


    //Adding Lead Source
    @PostMapping("/LeadSource")
    public ResponseEntity<LeadSource> saveLeadSource(@RequestBody LeadSource leadSource) {
        return ResponseEntity.ok(leadSourceRepository.save(leadSource));
    }

    //Getting All Deal Category
    @GetMapping
    public ResponseEntity<List<DealCategory>> findAllDealCategory() {
        return ResponseEntity.ok(dealcCategoryRepository.findAll());
    }

    //Getting All LeadSource
    @GetMapping("/LeadSource")
    public ResponseEntity<List<LeadSource>> findAllLeadSource() {
        return ResponseEntity.ok(leadSourceRepository.findAll());
    }

    //Deleting DealCategory
    @DeleteMapping("/{dealCategoryId}")
    public String deleteDealCategory(@PathVariable Long dealCategoryId) {
        dealcCategoryRepository.deleteById(dealCategoryId);
        return "Successfull Delete";
    }

    //Deleting LeadCategory
    @DeleteMapping("/LeadSource/{leadSourceId}")
    public String deleteLeadSource(@PathVariable Long leadSourceId) {
        leadSourceRepository.deleteById(leadSourceId);
        return "Successfull Delete";
    }

}
