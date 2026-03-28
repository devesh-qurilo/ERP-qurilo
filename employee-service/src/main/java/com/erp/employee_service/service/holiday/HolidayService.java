package com.erp.employee_service.service.holiday;

import com.erp.employee_service.dto.holiday.BulkHolidayRequestDto;
import com.erp.employee_service.dto.holiday.DefaultHolidaysRequestDto;
import com.erp.employee_service.dto.holiday.HolidayRequestDto;
import com.erp.employee_service.dto.holiday.HolidayResponseDto;
import com.erp.employee_service.entity.holiday.Holiday;
import com.erp.employee_service.exception.ResourceNotFoundException;
import com.erp.employee_service.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public HolidayResponseDto createHoliday(HolidayRequestDto requestDto) {
        // Check if holiday already exists for this date
        if (holidayRepository.existsByDate(requestDto.getDate())) {
            throw new IllegalArgumentException("Holiday already exists for date: " + requestDto.getDate());
        }

        String dayName = getDayName(requestDto.getDate());

        Holiday holiday = Holiday.builder()
                .date(requestDto.getDate())
                .day(dayName)
                .occasion(requestDto.getOccasion())
                .isDefaultWeekly(false)
                .isActive(true)
                .build();

        Holiday savedHoliday = holidayRepository.save(holiday);
        return mapToResponseDto(savedHoliday);
    }

    public List<HolidayResponseDto> createBulkHolidays(BulkHolidayRequestDto requestDto) {
        return requestDto.getHolidays().stream()
                .map(holidayDto -> {
                    // Skip if holiday already exists for this date
                    if (holidayRepository.existsByDate(holidayDto.getDate())) {
                        return null;
                    }

                    String dayName = getDayName(holidayDto.getDate());

                    Holiday holiday = Holiday.builder()
                            .date(holidayDto.getDate())
                            .day(dayName)
                            .occasion(holidayDto.getOccasion())
                            .isDefaultWeekly(false)
                            .isActive(true)
                            .build();

                    return holidayRepository.save(holiday);
                })
                .filter(holiday -> holiday != null)
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<HolidayResponseDto> setDefaultWeeklyHolidays(DefaultHolidaysRequestDto requestDto) {
        int year = requestDto.getYear() != null ? requestDto.getYear() : LocalDate.now().getYear();
        int month = requestDto.getMonth() != null ? requestDto.getMonth() : LocalDate.now().getMonthValue();
        String occasion = requestDto.getOccasion() != null ? requestDto.getOccasion() : "Weekly Holiday";

        // Get start and end of the target month
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // Delete existing default weekly holidays for this month
        List<Holiday> existingDefaults = holidayRepository.findByDateBetween(startDate, endDate)
                .stream()
                .filter(Holiday::getIsDefaultWeekly)
                .collect(Collectors.toList());
        holidayRepository.deleteAll(existingDefaults);

        // Create new default weekly holidays for the specified month
        return requestDto.getWeekDays().stream()
                .map(dayName -> createWeeklyHolidaysForMonth(dayName, year, month, occasion))
                .flatMap(List::stream)
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private List<Holiday> createWeeklyHolidaysForMonth(String dayName, int year, int month, String occasion) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(dayName.toUpperCase());

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return startDate.datesUntil(endDate.plusDays(1))
                .filter(date -> date.getDayOfWeek() == dayOfWeek)
                .map(date -> {
                    Holiday holiday = Holiday.builder()
                            .date(date)
                            .day(dayName)
                            .occasion(occasion)
                            .isDefaultWeekly(true)
                            .isActive(true)
                            .build();
                    return holidayRepository.save(holiday);
                })
                .collect(Collectors.toList());
    }

    public List<HolidayResponseDto> getAllHolidays() {
        return holidayRepository.findAllByOrderByDateAsc()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<HolidayResponseDto> getHolidaysByMonth(int year, int month) {
        return holidayRepository.findByYearAndMonth(year, month)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public List<HolidayResponseDto> getUpcomingHolidays() {
        LocalDate today = LocalDate.now();
        return holidayRepository.findUpcomingHolidays(today)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    public HolidayResponseDto getHolidayById(Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with ID: " + id));
        return mapToResponseDto(holiday);
    }

    public HolidayResponseDto updateHoliday(Long id, HolidayRequestDto requestDto) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with ID: " + id));

        // Check if date is being changed and if new date already exists
        if (!holiday.getDate().equals(requestDto.getDate()) &&
                holidayRepository.existsByDate(requestDto.getDate())) {
            throw new IllegalArgumentException("Holiday already exists for date: " + requestDto.getDate());
        }

        holiday.setDate(requestDto.getDate());
        holiday.setDay(getDayName(requestDto.getDate()));
        holiday.setOccasion(requestDto.getOccasion());

        Holiday updatedHoliday = holidayRepository.save(holiday);
        return mapToResponseDto(updatedHoliday);
    }

    public void deleteHoliday(Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with ID: " + id));
        holidayRepository.delete(holiday);
    }

    public void toggleHolidayStatus(Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with ID: " + id));
        holiday.setIsActive(!holiday.getIsActive());
        holidayRepository.save(holiday);
    }

    private String getDayName(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private HolidayResponseDto mapToResponseDto(Holiday holiday) {
        HolidayResponseDto dto = new HolidayResponseDto();
        dto.setId(holiday.getId());
        dto.setDate(holiday.getDate());
        dto.setDay(holiday.getDay());
        dto.setOccasion(holiday.getOccasion());
        dto.setIsDefaultWeekly(holiday.getIsDefaultWeekly());
        dto.setIsActive(holiday.getIsActive());
        return dto;
    }
}