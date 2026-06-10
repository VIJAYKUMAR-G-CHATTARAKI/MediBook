package com.medibook.medibookservice.repository;

import com.medibook.medibookservice.entity.Booking;
import com.medibook.medibookservice.entity.DoctorSlot;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Booking entity.
 *
 * <p>Includes specialized methods for:</p>
 * <ul>
 *   <li>Concurrency-safe booking creation (PESSIMISTIC_WRITE)</li>
 *   <li>Duplicate detection (idempotency)</li>
 *   <li>Patient/Doctor/Admin querying</li>
 *   <li>Booking reference generation support</li>
 * </ul>
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>,
        JpaSpecificationExecutor<Booking> {

    // =========================================================
    // CONCURRENCY-SAFE LOOKUP (the critical one!)
    // =========================================================

    /**
     * Load a booking with PESSIMISTIC_WRITE lock.
     * Other transactions trying to read/update this row will WAIT.
     * Use within @Transactional method when modifying the booking.
     *
     * <p>Timeout: 5 seconds. After that, lock acquisition fails fast
     * instead of hanging forever.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")
    })
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);

    // =========================================================
    // DUPLICATE DETECTION (idempotency)
    // =========================================================

    /**
     * Check if patient already has a booking for this slot.
     * Used during booking creation to enforce uniqueness BEFORE
     * the database constraint kicks in (better error message).
     */
    boolean existsByPatientUserIdAndSlotIdAndStatusIn(
            Long patientUserId, Long slotId, List<BookingStatus> statuses);

    /**
     * Find a specific patient's booking for a specific slot.
     * Returns existing booking if any, regardless of status.
     */
    Optional<Booking> findByPatientUserIdAndSlotId(Long patientUserId, Long slotId);

    // =========================================================
    // BOOKING REFERENCE
    // =========================================================

    /**
     * Look up a booking by its human-readable reference (e.g., "DR5-20261209-001").
     */
    Optional<Booking> findByBookingReference(String bookingReference);

    /**
     * Check if a reference already exists.
     * Used by reference generator to avoid collisions.
     */
    boolean existsByBookingReference(String bookingReference);

    // =========================================================
    // SEQUENCE COUNT (for booking reference generation)
    // =========================================================

    /**
     * Count existing bookings for a doctor on a specific slot date.
     * Used to compute the next sequence number for booking reference.
     * Example: 5 bookings exist for Dr.5 on Dec 9 → next reference is DR5-20261209-006.
     *
     * Includes ALL statuses (cancelled bookings still consumed a sequence number).
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.slot.doctor.id = :doctorId " +
            "AND b.slot.slotDate = :slotDate")
    long countByDoctorIdAndSlotDate(
            @Param("doctorId") Long doctorId,
            @Param("slotDate") LocalDate slotDate);

    // =========================================================
    // PATIENT QUERIES
    // =========================================================

    /**
     * Find all bookings for a patient (paginated).
     */
    Page<Booking> findByPatientUser(User patientUser, Pageable pageable);

    /**
     * Find all bookings for a patient with specific statuses.
     */
    Page<Booking> findByPatientUserAndStatusIn(
            User patientUser, List<BookingStatus> statuses, Pageable pageable);

    /**
     * Find patient's UPCOMING bookings (future slots, status BOOKED).
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.patientUser.id = :patientUserId " +
            "AND b.status = 'BOOKED' " +
            "AND b.slot.slotDate >= :today " +
            "ORDER BY b.slot.slotDate ASC, b.slot.startTime ASC")
    List<Booking> findUpcomingBookings(
            @Param("patientUserId") Long patientUserId,
            @Param("today") LocalDate today);

    // =========================================================
    // DOCTOR QUERIES (via slot relationship)
    // =========================================================

    /**
     * Find all bookings for a specific slot.
     */
    List<Booking> findBySlot(DoctorSlot slot);

    /**
     * Find bookings for a slot with specific statuses.
     */
    List<Booking> findBySlotAndStatusIn(DoctorSlot slot, List<BookingStatus> statuses);

    /**
     * Find bookings for a doctor in a date range (paginated).
     * Goes through slot to find bookings for that doctor.
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.slot.doctor.id = :doctorId " +
            "AND b.slot.slotDate BETWEEN :startDate AND :endDate")
    Page<Booking> findByDoctorIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Find doctor's bookings for a specific slot (by slot ID, more efficient).
     */
    @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId")
    List<Booking> findAllBySlotId(@Param("slotId") Long slotId);

    // =========================================================
    // STATISTICS & ANALYTICS
    // =========================================================

    /**
     * Count bookings for a doctor by status.
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.slot.doctor.id = :doctorId " +
            "AND b.status = :status")
    long countByDoctorIdAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("status") BookingStatus status);

    /**
     * Count bookings for a patient by status.
     */
    long countByPatientUserAndStatus(User patientUser, BookingStatus status);

    /**
     * Count active bookings (BOOKED) on a specific slot.
     * Used to verify slot capacity in real-time.
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.slot.id = :slotId " +
            "AND b.status = 'BOOKED'")
    long countActiveBookingsForSlot(@Param("slotId") Long slotId);

    // =========================================================
    // CLEANUP / SCHEDULED JOBS (Phase 5 preview)
    // =========================================================

    /**
     * Find BOOKED bookings where the slot's end time has passed.
     * Candidates for auto-marking as NO_SHOW (Phase 5 scheduled job).
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.status = 'BOOKED' " +
            "AND b.slot.slotDate < :today " +
            "OR (b.slot.slotDate = :today AND b.slot.endTime < :currentTime)")
    List<Booking> findExpiredBookings(
            @Param("today") LocalDate today,
            @Param("currentTime") java.time.LocalTime currentTime);
}