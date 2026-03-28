package com.erp.employee_service.service.designation.impl;

import com.erp.employee_service.dto.designation.DesignationCreateDto;
import com.erp.employee_service.dto.designation.DesignationResponseDto;
import com.erp.employee_service.dto.designation.DesignationUpdateDto;
import com.erp.employee_service.entity.designation.Designation;
import com.erp.employee_service.repository.DesignationRepository;
import com.erp.employee_service.service.designation.DesignationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DesignationServiceImpl implements DesignationService {

    private final DesignationRepository repo;

    public DesignationServiceImpl(DesignationRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public DesignationResponseDto create(DesignationCreateDto dto) {
        Designation d = new Designation();
        d.setDesignationName(dto.getDesignationName().trim());

        if (dto.getParentDesignationId() != null) {
            Designation parent = repo.findById(dto.getParentDesignationId())
                    .orElseThrow(() -> new RuntimeException("Parent designation not found"));
            d.setParentDesignation(parent);
        }

        return map(repo.save(d));
    }

    @Override
    @Transactional
    public DesignationResponseDto update(Long id, DesignationUpdateDto dto) {
        Designation d = repo.findById(id).orElseThrow(() -> new RuntimeException("Designation not found"));
        if (dto.getDesignationName() != null) d.setDesignationName(dto.getDesignationName().trim());

        if (dto.getParentDesignationId() != null) {
            Designation parent = repo.findById(dto.getParentDesignationId())
                    .orElseThrow(() -> new RuntimeException("Parent designation not found"));
            d.setParentDesignation(parent);
        }
        else{
            d.setParentDesignation(null);
        }

        return map(repo.save(d));
    }

    @Override
    public DesignationResponseDto getById(Long id) {
        return map(repo.findById(id).orElseThrow(() -> new RuntimeException("Designation not found")));
    }

    @Override
    public List<DesignationResponseDto> getAll() {
        return repo.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Designation d = repo.findById(id).orElseThrow(() -> new RuntimeException("Designation not found"));
        if (d != null) {
            repo.delete(d);
        }
    }

    private DesignationResponseDto map(Designation d) {
        DesignationResponseDto.DesignationResponseDtoBuilder b = DesignationResponseDto.builder()
                .id(d.getId())
                .designationName(d.getDesignationName())
                .createDate(d.getCreateDate());
        if (d.getParentDesignation() != null) {
            b.parentDesignationId(d.getParentDesignation().getId())
                    .parentDesignationName(d.getParentDesignation().getDesignationName());
        }
        return b.build();
    }
}
