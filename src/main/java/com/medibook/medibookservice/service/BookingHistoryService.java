package com.medibook.medibookservice.service;

import com.medibook.medibookservice.dto.response.BookingHistoryResponse;
import com.medibook.medibookservice.enums.BookingHistoryAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for querying the booking audit trail.
 *
 * <p>Provides read-only access to BookingHistory entries.
 * History creation happens in {@link BookingService} as part of state transitions.</p>
 */
public interface BookingHistoryService {

    /**
     * Get the complete timeline of events for a booking.
     */
    List<BookingHistoryResponse> getBookingTimeline(Long bookingId);

    /**
     * Get a user's recent actions (audit log for one user).
     */
    Page<BookingHistoryResponse> getUserActivity(Long userId, Pageable pageable);

    /**
     * Get events of a specific action within a time range (analytics).
     */
    Page<BookingHistoryResponse> getActionsInRange(
            BookingHistoryAction action,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable);

    /**
     * Count actions of a type within a time range.
     */
    long countActionsInRange(
            BookingHistoryAction action,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime);

    /**
     * Count actions by doctor and type within a time range.
     */
    long countActionsByDoctorInRange(
            BookingHistoryAction action,
            Long doctorId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime);
}