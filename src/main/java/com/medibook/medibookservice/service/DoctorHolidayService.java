package com.medibook.medibookservice.service;

import com.medibook.medibookservice.dto.request.BulkHolidayRequest;
import com.medibook.medibookservice.dto.request.HolidayRequest;
import com.medibook.medibookservice.dto.response.HolidayResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for managing doctor holidays (admin operations).
 *
 * <p>Per design: adding a holiday also auto-cancels any existing slots for that date.
 * This is the "Holiday entity + auto-cancel slots" approach.</p>
 */
public interface DoctorHolidayService {

    /**
     * Add a single holiday for a doctor.
     * If existing slots exist for that date AND request.cancelExistingSlots is true,
     * those slots are bulk-cancelled.
     */
    HolidayResponse addHoliday(Long doctorId, HolidayRequest request, Long adminUserId);

    /**
     * Add multiple holidays at once (e.g., vacation week).
     * Returns the list of created holiday responses.
     */
    List<HolidayResponse> addBulkHolidays(Long doctorId, BulkHolidayRequest request, Long adminUserId);

    /**
     * Remove a holiday entry.
     * Note: previously-cancelled slots remain cancelled. Admin must re-generate
     * slots for that date if needed.
     */
    void removeHoliday(Long holidayId);

    /**
     * Get all holidays for a doctor (paginated).
     */
    Page<HolidayResponse> getHolidaysForDoctor(Long doctorId, Pageable pageable);

    /**
     * Get upcoming holidays for a doctor (today and future).
     */
    List<HolidayResponse> getUpcomingHolidays(Long doctorId);
}