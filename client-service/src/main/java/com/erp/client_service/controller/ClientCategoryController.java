package com.erp.client_service.controller;

import com.erp.client_service.entity.ClientCategory;
import com.erp.client_service.entity.ClientSubCategory;
import com.erp.client_service.repository.ClientCategoryRepository;
import com.erp.client_service.repository.ClientSubCategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients/category")
@AllArgsConstructor
public class ClientCategoryController {
    private final ClientCategoryRepository clientCategoryRepository;
    private final ClientSubCategoryRepository clientSubCategoryRepository;

    // Create category
    @PostMapping
    public ResponseEntity<ClientCategory> saveCategory(@RequestBody ClientCategory clientCategory) {
        return ResponseEntity.ok(clientCategoryRepository.save(clientCategory));
    }

    // Create subcategory
    @PostMapping("/subcategory")
    public ResponseEntity<ClientSubCategory> saveSubCategory(@RequestBody ClientSubCategory subCategory) {
        return ResponseEntity.ok(clientSubCategoryRepository.save(subCategory));
    }

    // Get all categories
    @GetMapping
    public ResponseEntity<List<ClientCategory>> findAllCategories() {
        return ResponseEntity.ok(clientCategoryRepository.findAll());
    }

    // Get all subcategories
    @GetMapping("/subcategory")
    public ResponseEntity<List<ClientSubCategory>> findAllSubCategories() {
        return ResponseEntity.ok(clientSubCategoryRepository.findAll());
    }

    // Delete category by id
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategoryById(@PathVariable Long id) {
        clientCategoryRepository.deleteById(id);
        return ResponseEntity.ok("SUCCESS");
    }

    // Delete subcategory by id
    @DeleteMapping("/subcategory/{id}")
    public ResponseEntity<String> deleteSubCategoryById(@PathVariable Long id) {
        clientSubCategoryRepository.deleteById(id);
        return ResponseEntity.ok("SUCCESS");
    }
}
