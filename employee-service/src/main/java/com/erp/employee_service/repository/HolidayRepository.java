package com.erp.employee_service.repository;

import com.erp.employee_service.entity.holiday.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Holiday> findByIsDefaultWeeklyTrue();

    List<Holiday> findByOccasionContainingIgnoreCase(String occasion);

    @Query("SELECT h FROM Holiday h WHERE YEAR(h.date) = :year AND MONTH(h.date) = :month")
    List<Holiday> findByYearAndMonth(int year, int month);

    List<Holiday> findAllByOrderByDateAsc();

    boolean existsByDate(LocalDate date);

    @Query("SELECT h FROM Holiday h WHERE h.date >= :startDate ORDER BY h.date ASC")
    List<Holiday> findUpcomingHolidays(LocalDate startDate);

    //Attendance Management
    List<Holiday> findByDateAndIsActiveTrue(LocalDate date);
}