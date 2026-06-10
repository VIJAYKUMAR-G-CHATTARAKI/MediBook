package com.medibook.medibookservice.service;

import com.medibook.medibookservice.dto.request.CancelSlotRequest;
import com.medibook.medibookservice.dto.response.SlotResponse;
import com.medibook.medibookservice.dto.response.SlotSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for slot operations (viewing, cancelling individual slots).
 *
 * <p>Different methods serve different audiences:</p>
 * <ul>
 *   <li>Authenticated (Doctor/Admin) - full slot details with audit info</li>
 *   <li>Public - only AVAILABLE slots, minimal info</li>
 * </ul>
 */
public interface DoctorSlotService {

    // =========================================================
    // AUTHENTICATED VIEWS (Doctor/Admin)
    // =========================================================

    /**
     * Get all slots for a doctor on a specific date.
     * Returns full details (including cancelled/expired slots).
     */
    List<SlotResponse> getSlotsForDoctorByDate(Long doctorId, LocalDate date);

    /**
     * Get slots for a doctor in a date range (paginated).
     */
    Page<SlotResponse> getSlotsForDoctorInRange(
            Long doctorId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Cancel a specific slot.
     */
    SlotResponse cancelSlot(Long slotId, CancelSlotRequest request, Long cancelledByUserId);

    /**
     * Get a single slot by ID (for admin/doctor).
     */
    SlotResponse getSlotById(Long slotId);

    // =========================================================
    // PUBLIC VIEWS (Patient-facing)
    // =========================================================

    /**
     * Get AVAILABLE slots for a doctor on a date.
     * Returns minimal summary - only AVAILABLE status.
     */
    List<SlotSummaryResponse> getAvailableSlotsByDoctorAndDate(Long doctorId, LocalDate date);

    /**
     * Get AVAILABLE slots for a doctor in a date range (e.g., next 7 days).
     */
    List<SlotSummaryResponse> getAvailableSlotsInRange(
            Long doctorId, LocalDate startDate, LocalDate endDate);

    /**
     * Get next N available slots for a doctor (e.g., "show me 5 upcoming slots").
     */
    List<SlotSummaryResponse> getNextAvailableSlots(Long doctorId, int limit);

    /**
     * Get distinct dates with availability for a doctor (calendar UI).
     */
    List<LocalDate> getDatesWithAvailability(Long doctorId, LocalDate fromDate);
}