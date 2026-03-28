package com.erp.project_service.mapper;

import com.erp.project_service.dto.timesheet.TimeLogCreateRequest;
import com.erp.project_service.dto.timesheet.TimeLogDto;
import com.erp.project_service.entity.TimeLog;

public final class TimeLogMapper {
    private TimeLogMapper() {}

    public static TimeLog toEntity(TimeLogCreateRequest r) {
        if (r == null) return null;
        TimeLog t = TimeLog.builder()
                .projectId(r.getProjectId())
                .taskId(r.getTaskId())
                .employeeId(r.getEmployeeId())
                .startDate(r.getStartDate())
                .startTime(r.getStartTime())
                .endDate(r.getEndDate())
                .endTime(r.getEndTime())
                .memo(r.getMemo())
                .build();
        return t;
    }

    public static TimeLogDto toDto(TimeLog e) {
        if (e == null) return null;
        TimeLogDto d = new TimeLogDto();
        d.setId(e.getId());
        d.setProjectId(e.getProjectId());
        d.setTaskId(e.getTaskId());
        d.setEmployeeId(e.getEmployeeId());
        d.setStartDate(e.getStartDate());
        d.setStartTime(e.getStartTime());
        d.setEndDate(e.getEndDate());
        d.setEndTime(e.getEndTime());
        d.setMemo(e.getMemo());

        // Convert minutes to hours for durationHours
        if (e.getDurationMinutes() != null) {
            d.setDurationHours(e.getDurationMinutes() / 60);
        } else {
            d.setDurationHours(0L);
        }

        d.setCreatedBy(e.getCreatedBy());
        d.setCreatedAt(e.getCreatedAt());

        // Note: employees list will be set separately in service layer
        return d;
    }
}