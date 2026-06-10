package com.medibook.medibookservice.mapper;

import com.medibook.medibookservice.dto.response.SlotResponse;
import com.medibook.medibookservice.dto.response.SlotSummaryResponse;
import com.medibook.medibookservice.entity.DoctorSlot;
import org.springframework.stereotype.Component;

/**
 * Mapper for DoctorSlot entity.
 *
 * <p>Provides two response variants:</p>
 * <ul>
 *   <li>{@link SlotResponse} - Full details for doctor and admin views</li>
 *   <li>{@link SlotSummaryResponse} - Minimal data for public listings</li>
 * </ul>
 */
@Component
public class SlotMapper {

    // =========================================================
    // ENTITY -> FULL RESPONSE (for authenticated views)
    // =========================================================

    public SlotResponse toResponse(DoctorSlot slot) {
        if (slot == null) return null;

        SlotResponse response = new SlotResponse();
        response.setId(slot.getId());

        if (slot.getDoctor() != null) {
            response.setDoctorId(slot.getDoctor().getId());
            response.setDoctorName(slot.getDoctor().getFullName());
        }

        response.setSlotDate(slot.getSlotDate());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setMaxBookings(slot.getMaxBookings());
        response.setCurrentBookings(slot.getCurrentBookings());
        response.setRemainingSpots(slot.getRemainingSpots());
        response.setStatus(slot.getStatus());
        response.setCancellationReason(slot.getCancellationReason());
        response.setCancelledByUserId(slot.getCancelledByUserId());
        response.setCancelledAt(slot.getCancelledAt());
        response.setCreatedAt(slot.getCreatedAt());
        response.setUpdatedAt(slot.getUpdatedAt());

        return response;
    }

    // =========================================================
    // ENTITY -> SUMMARY RESPONSE (for public listings)
    // =========================================================

    public SlotSummaryResponse toSummaryResponse(DoctorSlot slot) {
        if (slot == null) return null;

        SlotSummaryResponse response = new SlotSummaryResponse();
        response.setId(slot.getId());
        response.setSlotDate(slot.getSlotDate());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setRemainingSpots(slot.getRemainingSpots());

        return response;
    }
}