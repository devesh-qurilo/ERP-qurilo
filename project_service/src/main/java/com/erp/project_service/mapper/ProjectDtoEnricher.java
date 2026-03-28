//package com.erp.project_service.mapper;
//
//import com.erp.project_service.dto.common.EmployeeMetaDto;
//import com.erp.project_service.dto.project.ProjectDto;
//import com.erp.project_service.entity.ProjectUserState;
//import com.erp.project_service.repository.ProjectUserStateRepository;
//import com.erp.project_service.client.EmployeeClient;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class ProjectDtoEnricher {
//    private final ProjectUserStateRepository stateRepo;
//    private final EmployeeClient employeeClient; // injected client
//
//    public ProjectDto enrichOne(ProjectDto dto, String userId) {
//        if (dto == null || dto.getId() == null) return dto;
//        return enrichMany(Collections.singletonList(dto), userId).get(0);
//    }
//
//    public List<ProjectDto> enrichMany(List<ProjectDto> dtos, String userId) {
//        if (dtos == null || dtos.isEmpty()) return dtos;
//
//        // Resolve requester id (use provided userId or fallback to SecurityContext)
//        String resolvedRequester = resolveRequesterEmployeeId(userId);
//        log.debug("ProjectDtoEnricher: resolvedRequester='{}' for provided userId='{}'", resolvedRequester, userId);
//
//        // 1) existing user state enrichment (pins/archives) using resolvedRequester (null-safe)
//        List<Long> ids = dtos.stream().map(ProjectDto::getId).filter(Objects::nonNull).collect(Collectors.toList());
//        Map<Long, ProjectUserState> byProject = new HashMap<>();
//        if (!ids.isEmpty() && resolvedRequester != null) {
//            byProject = stateRepo
//                    .findByUserIdAndProjectIdIn(resolvedRequester, ids)
//                    .stream().collect(Collectors.toMap(ProjectUserState::getProjectId, Function.identity()));
//        }
//
//        for (ProjectDto d : dtos) {
//            ProjectUserState s = byProject.get(d.getId());
//            if (s != null) {
//                d.setPinned(s.getPinnedAt() != null);
//                d.setPinnedAt(s.getPinnedAt());
//                d.setArchived(s.getArchivedAt() != null);
//                d.setArchivedAt(s.getArchivedAt());
//            } else {
//                d.setPinned(false);
//                d.setPinnedAt(null);
//                d.setArchived(false);
//                d.setArchivedAt(null);
//            }
//        }
//
//        // 2) project-admin enrichment + isRequesterProjectAdmin flag
//        Set<String> adminIds = dtos.stream()
//                .map(ProjectDto::getProjectAdminId)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toSet());
//
//        Map<String, EmployeeMetaDto> adminMetaMap = new HashMap<>();
//        if (!adminIds.isEmpty()) {
//            for (String adminId : adminIds) {
//                try {
//                    EmployeeMetaDto meta = employeeClient.getMeta(adminId);
//                    if (meta != null) adminMetaMap.put(adminId, meta);
//                } catch (Exception ex) {
//                    log.warn("Failed to fetch admin meta for {}: {}", adminId, ex.getMessage());
//                }
//            }
//        }
//
//        for (ProjectDto d : dtos) {
//            String adminIdRaw = d.getProjectAdminId();
//            String adminId = adminIdRaw == null ? null : adminIdRaw.trim();
//            if (adminId != null) {
//                d.setProjectAdmin(adminMetaMap.get(adminId));
//            } else {
//                d.setProjectAdmin(null);
//            }
//
//            String reqId = resolvedRequester == null ? null : resolvedRequester.trim();
//            boolean isRequesterAdmin = false;
//            if (adminId != null && reqId != null) {
//                isRequesterAdmin = adminId.equals(reqId) || adminId.equalsIgnoreCase(reqId);
//            }
//            d.setIsRequesterProjectAdmin(Boolean.valueOf(isRequesterAdmin));
//
//            log.debug("ProjectId={} adminId='{}' requester='{}' => isRequesterProjectAdmin={}",
//                    d.getId(), adminId, reqId, isRequesterAdmin);
//        }
//
//        return dtos;
//    }
//
//    // --- helper to get a usable requester employee id (fallback if provided userId null or different)
//    private String resolveRequesterEmployeeId(String requesterId) {
//        if (requesterId != null && !requesterId.isBlank()) return requesterId.trim();
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null) return null;
//
//        Object principal = auth.getPrincipal();
//        try {
//            if (principal == null) return null;
//            String s = principal.toString();
//            if (s != null && !s.isBlank()) return s.trim();
//        } catch (Exception ignore) { }
//        return null;
//    }
//}

package com.erp.project_service.mapper;

import com.erp.project_service.dto.common.EmployeeMetaDto;
import com.erp.project_service.dto.project.ProjectDto;
import com.erp.project_service.entity.ProjectUserState;
import com.erp.project_service.repository.ProjectUserStateRepository;
import com.erp.project_service.client.EmployeeClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectDtoEnricher {
    private final ProjectUserStateRepository stateRepo;
    private final EmployeeClient employeeClient; // injected client

    public ProjectDto enrichOne(ProjectDto dto, String userId) {
        if (dto == null || dto.getId() == null) return dto;
        return enrichMany(Collections.singletonList(dto), userId).get(0);
    }

    public List<ProjectDto> enrichMany(List<ProjectDto> dtos, String userId) {
        if (dtos == null || dtos.isEmpty()) return dtos;

        String resolvedRequester = resolveRequesterEmployeeId(userId);
        log.debug("ProjectDtoEnricher: resolvedRequester='{}' for provided userId='{}'", resolvedRequester, userId);

        // 1) user state enrichment
        List<Long> ids = dtos.stream().map(ProjectDto::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, ProjectUserState> byProject = new HashMap<>();
        if (!ids.isEmpty() && resolvedRequester != null) {
            byProject = stateRepo
                    .findByUserIdAndProjectIdIn(resolvedRequester, ids)
                    .stream().collect(Collectors.toMap(ProjectUserState::getProjectId, Function.identity()));
        }

        for (ProjectDto d : dtos) {
            ProjectUserState s = byProject.get(d.getId());
            if (s != null) {
                d.setPinned(s.getPinnedAt() != null);
                d.setPinnedAt(s.getPinnedAt());
                d.setArchived(s.getArchivedAt() != null);
                d.setArchivedAt(s.getArchivedAt());
            } else {
                d.setPinned(false);
                d.setPinnedAt(null);
                d.setArchived(false);
                d.setArchivedAt(null);
            }
        }

        // 2) project-admin enrichment + isRequesterProjectAdmin flag
        Set<String> adminIds = dtos.stream()
                .map(ProjectDto::getProjectAdminId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, EmployeeMetaDto> adminMetaMap = new HashMap<>();
        if (!adminIds.isEmpty()) {
            for (String adminId : adminIds) {
                EmployeeMetaDto meta = safeGetEmployeeMeta(adminId);
                if (meta != null) adminMetaMap.put(adminId, meta);
            }
        }

        for (ProjectDto d : dtos) {
            String adminIdRaw = d.getProjectAdminId();
            String adminId = adminIdRaw == null ? null : adminIdRaw.trim();
            if (adminId != null) {
                d.setProjectAdmin(adminMetaMap.get(adminId));
            } else {
                d.setProjectAdmin(null);
            }

            String reqId = resolvedRequester == null ? null : resolvedRequester.trim();
            boolean isRequesterAdmin = false;
            if (adminId != null && reqId != null) {
                isRequesterAdmin = adminId.equals(reqId) || adminId.equalsIgnoreCase(reqId);
            }
            d.setIsRequesterProjectAdmin(Boolean.valueOf(isRequesterAdmin));

            log.debug("ProjectId={} adminId='{}' requester='{}' => isRequesterProjectAdmin={}",
                    d.getId(), adminId, reqId, isRequesterAdmin);
        }

        return dtos;
    }

    // --- helper to get a usable requester employee id
    private String resolveRequesterEmployeeId(String requesterId) {
        if (requesterId != null && !requesterId.isBlank()) return requesterId.trim();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;

        Object principal = auth.getPrincipal();
        try {
            if (principal == null) return null;
            String s = principal.toString();
            if (s != null && !s.isBlank()) return s.trim();
        } catch (Exception ignore) { }
        return null;
    }

    // safe wrapper for employeeClient.getMeta to avoid bubbling exceptions
    private EmployeeMetaDto safeGetEmployeeMeta(String employeeId) {
        if (employeeId == null) return null;
        try {
            return employeeClient.getMeta(employeeId);
        } catch (Exception ex) {
            log.debug("ProjectDtoEnricher.safeGetEmployeeMeta: failed for {} -> {}", employeeId, ex.getMessage());
            return null;
        }
    }
}
