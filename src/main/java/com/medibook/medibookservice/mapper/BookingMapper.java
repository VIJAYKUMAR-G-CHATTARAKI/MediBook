package com.medibook.medibookservice.mapper;

import com.medibook.medibookservice.dto.response.BookingResponse;
import com.medibook.medibookservice.dto.response.BookingSummaryResponse;
import com.medibook.medibookservice.entity.Booking;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorSlot;
import com.medibook.medibookservice.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for Booking entity to/from DTOs.
 *
 * <p>Provides two response variants:</p>
 * <ul>
 *   <li>{@link BookingResponse} - Full details for detail views</li>
 *   <li>{@link BookingSummaryResponse} - Compact for list views</li>
 * </ul>
 */
@Component
public class BookingMapper {

    // =========================================================
    // ENTITY -> FULL RESPONSE
    // =========================================================

    public BookingResponse toResponse(Booking booking) {
        if (booking == null) return null;

        BookingResponse response = new BookingResponse();

        // Booking identifiers
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setStatus(booking.getStatus());

        // Patient info (denormalized)
        User patient = booking.getPatientUser();
        if (patient != null) {
            response.setPatientUserId(patient.getId());
            response.setPatientName(patient.getFullName());
            response.setPatientEmail(patient.getEmail());
        }

        // Slot info (denormalized)
        DoctorSlot slot = booking.getSlot();
        if (slot != null) {
            response.setSlotId(slot.getId());
            response.setSlotDate(slot.getSlotDate());
            response.setSlotStartTime(slot.getStartTime());
            response.setSlotEndTime(slot.getEndTime());

            // Doctor info (denormalized through slot)
            Doctor doctor = slot.getDoctor();
            if (doctor != null) {
                response.setDoctorId(doctor.getId());
                response.setDoctorName(doctor.getFullName());
                response.setDoctorSpecialization(doctor.getSpecialization());
            }
        }

        // Patient notes
        response.setNotes(booking.getNotes());

        // Cancellation audit (will be null if not cancelled)
        response.setCancellationReason(booking.getCancellationReason());
        response.setCancelledByUserId(booking.getCancelledByUserId());
        response.setCancelledAt(booking.getCancelledAt());

        // Completion timestamp (null if not completed)
        response.setCompletedAt(booking.getCompletedAt());

        // Timestamps
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());

        return response;
    }

    // =========================================================
    // ENTITY -> SUMMARY RESPONSE
    // =========================================================

    public BookingSummaryResponse toSummaryResponse(Booking booking) {
        if (booking == null) return null;

        BookingSummaryResponse response = new BookingSummaryResponse();

        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setStatus(booking.getStatus());

        // Slot info
        DoctorSlot slot = booking.getSlot();
        if (slot != null) {
            response.setSlotDate(slot.getSlotDate());
            response.setSlotStartTime(slot.getStartTime());
            response.setSlotEndTime(slot.getEndTime());

            // Doctor name only (no full doctor object)
            Doctor doctor = slot.getDoctor();
            if (doctor != null) {
                response.setDoctorName(doctor.getFullName());
            }
        }

        // Patient name (for doctor/admin views)
        User patient = booking.getPatientUser();
        if (patient != null) {
            response.setPatientName(patient.getFullName());
        }

        return response;
    }
}