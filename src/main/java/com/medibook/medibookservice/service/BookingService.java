
package com.medibook.medibookservice.service;

import com.medibook.medibookservice.dto.request.CancelBookingRequest;
import com.medibook.medibookservice.dto.request.CompleteBookingRequest;
import com.medibook.medibookservice.dto.request.CreateBookingRequest;
import com.medibook.medibookservice.dto.response.BookingResponse;
import com.medibook.medibookservice.dto.response.BookingSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for booking lifecycle operations.
 *
 * <p>The critical operations (create, cancel, complete, no-show) use database
 * pessimistic locking to ensure concurrency safety.</p>
 */
public interface BookingService {

    /**
     * Create a new booking for the given patient.
     * Acquires pessimistic lock on the slot during creation.
     *
     * @throws BadRequestException if slot is not bookable, patient already booked,
     *                             or other validation failures
     */
    BookingResponse createBooking(Long patientUserId, CreateBookingRequest request);

    /**
     * Cancel a booking.
     * For patient cancellations: enforces 2-hour rule.
     * Doctor and admin cancellations bypass the 2-hour rule.
     *
     * @param cancelledByRole "PATIENT", "DOCTOR", or "ADMIN" - controls cancellation rules
     */
    BookingResponse cancelBooking(Long bookingId, CancelBookingRequest request,
                                  Long cancelledByUserId, String cancelledByRole);

    /**
     * Mark a booking as completed (doctor only).
     * Requires the slot's end time to have passed.
     */
    BookingResponse completeBooking(Long bookingId, CompleteBookingRequest request,
                                    Long doctorUserId);

    /**
     * Mark a booking as no-show (doctor only).
     * Requires the slot's end time to have passed.
     */
    BookingResponse markNoShow(Long bookingId, Long doctorUserId);

    /**
     * Get a booking by ID.
     */
    BookingResponse getBookingById(Long bookingId);

    /**
     * Get a booking by its human-readable reference.
     */
    BookingResponse getBookingByReference(String bookingReference);

    /**
     * List bookings for a patient (paginated).
     */
    Page<BookingSummaryResponse> getBookingsForPatient(Long patientUserId, Pageable pageable);

    /**
     * List bookings for a doctor across a date range (paginated).
     */
    Page<BookingSummaryResponse> getBookingsForDoctor(Long doctorId, Pageable pageable);
}