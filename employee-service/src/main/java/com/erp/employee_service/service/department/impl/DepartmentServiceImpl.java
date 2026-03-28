package com.erp.employee_service.service.department.impl;

import com.erp.employee_service.dto.department.DepartmentCreateDto;
import com.erp.employee_service.dto.department.DepartmentResponseDto;
import com.erp.employee_service.dto.department.DepartmentUpdateDto;
import com.erp.employee_service.entity.department.Department;
import com.erp.employee_service.repository.DepartmentRepository;
import com.erp.employee_service.service.department.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository repo;

    public DepartmentServiceImpl(DepartmentRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public DepartmentResponseDto create(DepartmentCreateDto dto) {
        Department dep = new Department();
        dep.setDepartmentName(dto.getDepartmentName().trim());

        if (dto.getParentDepartmentId() != null) {
            Department parent = repo.findById(dto.getParentDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Parent department not found"));
            dep.setParentDepartment(parent);
        }

        Department saved = repo.save(dep);
        return map(saved);
    }

    @Override
    @Transactional
    public DepartmentResponseDto update(Long id, DepartmentUpdateDto dto) {
        Department dep = repo.findById(id).orElseThrow(() -> new RuntimeException("Department not found"));
        if (dto.getDepartmentName() != null) dep.setDepartmentName(dto.getDepartmentName().trim());

        if (dto.getParentDepartmentId() != null) {
            Department parent = repo.findById(dto.getParentDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Parent department not found"));
            dep.setParentDepartment(parent);
        } else {
            dto.setParentDepartmentId(null);
            dep.setParentDepartment(null);
        }

        Department saved = repo.save(dep);
        return map(saved);
    }

    @Override
    public DepartmentResponseDto getById(Long id) {
        return map(repo.findById(id).orElseThrow(() -> new RuntimeException("Department not found")));
    }

    @Override
    public List<DepartmentResponseDto> getAll() {
        return repo.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Department dep = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // Find direct children and detach them
        List<Department> children = repo.findByParentDepartment(dep);
        if (!children.isEmpty()) {
            for (Department child : children) {
                child.setParentDepartment(null);
            }
            repo.saveAll(children);
        }

        // Now safe to delete this dept
        repo.delete(dep);
    }


    private DepartmentResponseDto map(Department d) {
        DepartmentResponseDto.DepartmentResponseDtoBuilder b = DepartmentResponseDto.builder()
                .id(d.getId())
                .departmentName(d.getDepartmentName())
                .createAt(d.getCreateAt());
        if (d.getParentDepartment() != null) {
            b.parentDepartmentId(d.getParentDepartment().getId())
                    .parentDepartmentName(d.getParentDepartment().getDepartmentName());
        }
        return b.build();
    }
}
