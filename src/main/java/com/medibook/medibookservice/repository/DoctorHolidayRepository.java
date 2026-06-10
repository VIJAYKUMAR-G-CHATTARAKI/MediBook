package com.medibook.medibookservice.repository;

import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorHoliday;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for DoctorHoliday - tracks doctor leave days.
 */
@Repository
public interface DoctorHolidayRepository extends JpaRepository<DoctorHoliday, Long> {

    /**
     * Find all holidays for a doctor, paginated.
     */
    Page<DoctorHoliday> findByDoctor(Doctor doctor, Pageable pageable);

    /**
     * Find a doctor's holidays in a date range.
     * Used by slot generation algorithm to skip holiday dates.
     */
    @Query("SELECT h FROM DoctorHoliday h " +
            "WHERE h.doctor = :doctor " +
            "AND h.holidayDate BETWEEN :startDate AND :endDate")
    List<DoctorHoliday> findByDoctorAndDateRange(
            @Param("doctor") Doctor doctor,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Check if a specific date is a holiday for a doctor.
     */
    boolean existsByDoctorAndHolidayDate(Doctor doctor, LocalDate holidayDate);

    /**
     * Find a specific holiday entry (used for deletion).
     */
    @Query("SELECT h FROM DoctorHoliday h " +
            "WHERE h.doctor.id = :doctorId AND h.holidayDate = :date")
    DoctorHoliday findByDoctorIdAndHolidayDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    /**
     * Find upcoming holidays for a doctor (today and future).
     */
    @Query("SELECT h FROM DoctorHoliday h " +
            "WHERE h.doctor = :doctor AND h.holidayDate >= :today " +
            "ORDER BY h.holidayDate ASC")
    List<DoctorHoliday> findUpcomingHolidays(
            @Param("doctor") Doctor doctor,
            @Param("today") LocalDate today);
}