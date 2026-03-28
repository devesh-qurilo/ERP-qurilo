package com.erp.project_service.mapper;

import com.erp.project_service.dto.task.TaskDto;
import com.erp.project_service.entity.TaskUserState;
import com.erp.project_service.repository.TaskUserStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskDtoEnricher {
    private final TaskUserStateRepository stateRepo;

    public TaskDto enrichOne(TaskDto dto, String userId) {
        if (dto == null || dto.getId() == null) return dto;
        return enrichMany(List.of(dto), userId).get(0);
    }

    public List<TaskDto> enrichMany(List<TaskDto> dtos, String userId) {
        if (dtos == null || dtos.isEmpty()) return dtos;
        List<Long> ids = dtos.stream().map(TaskDto::getId).filter(Objects::nonNull).toList();
        Map<Long, TaskUserState> map = stateRepo.findByUserIdAndTaskIdIn(userId, ids)
                .stream().collect(Collectors.toMap(TaskUserState::getTaskId, Function.identity()));

        for (TaskDto d : dtos) {
            TaskUserState s = map.get(d.getId());
            if (s != null) { d.setPinned(s.getPinnedAt()!=null); d.setPinnedAt(s.getPinnedAt()); }
            else { d.setPinned(false); d.setPinnedAt(null); }
        }
        return dtos;
    }
}
