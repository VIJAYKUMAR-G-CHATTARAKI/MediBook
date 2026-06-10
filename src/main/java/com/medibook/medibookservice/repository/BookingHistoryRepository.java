package com.medibook.medibookservice.repository;

import com.medibook.medibookservice.entity.Booking;
import com.medibook.medibookservice.entity.BookingHistory;
import com.medibook.medibookservice.enums.BookingHistoryAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for BookingHistory entity.
 *
 * <p>Read-heavy repository for the audit trail. No locks needed since
 * history rows are immutable (append-only).</p>
 *
 * <p><b>Common queries:</b></p>
 * <ul>
 *   <li>Booking timeline - all events for a booking</li>
 *   <li>User audit - what has user X done?</li>
 *   <li>Analytics - cancellations per period, no-show rates</li>
 * </ul>
 */
@Repository
public interface BookingHistoryRepository extends JpaRepository<BookingHistory, Long> {

    // =========================================================
    // BOOKING TIMELINE (most common use case)
    // =========================================================

    /**
     * Get the complete timeline of events for a booking, chronologically.
     * Used by the booking detail page to show history.
     */
    List<BookingHistory> findByBookingOrderByPerformedAtAsc(Booking booking);

    /**
     * Same as above but by ID (avoids loading Booking first).
     */
    @Query("SELECT bh FROM BookingHistory bh " +
            "WHERE bh.booking.id = :bookingId " +
            "ORDER BY bh.performedAt ASC")
    List<BookingHistory> findByBookingIdOrderByPerformedAtAsc(
            @Param("bookingId") Long bookingId);

    /**
     * Count events for a booking (quick check).
     */
    long countByBooking(Booking booking);

    // =========================================================
    // USER AUDIT
    // =========================================================

    /**
     * Find all actions performed by a specific user (paginated).
     * Used for user activity audit and compliance.
     */
    Page<BookingHistory> findByPerformedByUserIdOrderByPerformedAtDesc(
            Long performedByUserId, Pageable pageable);

    /**
     * Count actions by a user.
     */
    long countByPerformedByUserId(Long performedByUserId);

    // =========================================================
    // ANALYTICS (action + time range)
    // =========================================================

    /**
     * Find history events of a specific action within a time range.
     * Used for analytics dashboards.
     */
    Page<BookingHistory> findByActionAndPerformedAtBetween(
            BookingHistoryAction action,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable);

    /**
     * Count events of a specific action within a time range.
     * Example: "How many cancellations this month?"
     */
    long countByActionAndPerformedAtBetween(
            BookingHistoryAction action,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime);

    /**
     * Count cancellations for a specific doctor in a time range.
     * Useful for doctor performance metrics.
     */
    @Query("SELECT COUNT(bh) FROM BookingHistory bh " +
            "WHERE bh.action = :action " +
            "AND bh.booking.slot.doctor.id = :doctorId " +
            "AND bh.performedAt BETWEEN :startDateTime AND :endDateTime")
    long countByActionAndDoctorAndDateRange(
            @Param("action") BookingHistoryAction action,
            @Param("doctorId") Long doctorId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    // =========================================================
    // RECENT ACTIVITY
    // =========================================================

    /**
     * Get the most recent history events (across all bookings).
     * Used for an admin "recent activity" view.
     */
    @Query("SELECT bh FROM BookingHistory bh ORDER BY bh.performedAt DESC")
    Page<BookingHistory> findRecentActivity(Pageable pageable);
}