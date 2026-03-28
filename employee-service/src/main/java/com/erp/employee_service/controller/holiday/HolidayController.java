package com.erp.employee_service.controller.holiday;

import com.erp.employee_service.dto.holiday.BulkHolidayRequestDto;
import com.erp.employee_service.dto.holiday.DefaultHolidaysRequestDto;
import com.erp.employee_service.dto.holiday.HolidayRequestDto;
import com.erp.employee_service.dto.holiday.HolidayResponseDto;
import com.erp.employee_service.service.holiday.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employee/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    // Admin endpoints
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HolidayResponseDto> createHoliday(@Valid @RequestBody HolidayRequestDto requestDto) {
        HolidayResponseDto response = holidayService.createHoliday(requestDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HolidayResponseDto>> createBulkHolidays(@Valid @RequestBody BulkHolidayRequestDto requestDto) {
        List<HolidayResponseDto> responses = holidayService.createBulkHolidays(requestDto);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/default-weekly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HolidayResponseDto>> setDefaultWeeklyHolidays(@Valid @RequestBody DefaultHolidaysRequestDto requestDto) {
        List<HolidayResponseDto> responses = holidayService.setDefaultWeeklyHolidays(requestDto);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HolidayResponseDto> updateHoliday(@PathVariable Long id, @Valid @RequestBody HolidayRequestDto requestDto) {
        HolidayResponseDto response = holidayService.updateHoliday(id, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleHolidayStatus(@PathVariable Long id) {
        holidayService.toggleHolidayStatus(id);
        return ResponseEntity.noContent().build();
    }

    // Public endpoints (for all users)
    @GetMapping
    public ResponseEntity<List<HolidayResponseDto>> getAllHolidays() {
        List<HolidayResponseDto> holidays = holidayService.getAllHolidays();
        return ResponseEntity.ok(holidays);
    }

    @GetMapping("/month")
    public ResponseEntity<List<HolidayResponseDto>> getHolidaysByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        List<HolidayResponseDto> holidays = holidayService.getHolidaysByMonth(year, month);
        return ResponseEntity.ok(holidays);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<HolidayResponseDto>> getUpcomingHolidays() {
        List<HolidayResponseDto> holidays = holidayService.getUpcomingHolidays();
        return ResponseEntity.ok(holidays);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HolidayResponseDto> getHolidayById(@PathVariable Long id) {
        HolidayResponseDto holiday = holidayService.getHolidayById(id);
        return ResponseEntity.ok(holiday);
    }
}