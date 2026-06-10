package com.medibook.medibookservice.repository;

import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorSlot;
import com.medibook.medibookservice.enums.DoctorSlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DoctorSlot - the workhorse of slot operations.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Standard CRUD via JpaRepository</li>
 *   <li>Dynamic queries via JpaSpecificationExecutor (advanced filtering)</li>
 *   <li>Concurrency control via @Lock for booking (Phase 4)</li>
 * </ul>
 */
@Repository
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, Long>,
        JpaSpecificationExecutor<DoctorSlot> {

    // =========================================================
    // BASIC QUERIES (used by Doctor and Admin views)
    // =========================================================

    /**
     * Find all slots for a doctor on a specific date.
     */
    List<DoctorSlot> findByDoctorAndSlotDateOrderByStartTimeAsc(
            Doctor doctor, LocalDate slotDate);

    /**
     * Find slots in a date range for a doctor (paginated).
     */
    Page<DoctorSlot> findByDoctorAndSlotDateBetween(
            Doctor doctor, LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Check if a slot already exists for the unique combination.
     * Used during slot generation to prevent duplicates.
     */
    boolean existsByDoctorAndSlotDateAndStartTime(
            Doctor doctor, LocalDate slotDate, LocalTime startTime);

    // =========================================================
    // PUBLIC QUERIES (patient-facing, only AVAILABLE slots)
    // =========================================================

    /**
     * Find available slots for a doctor on a specific date.
     * Used by patients browsing availability.
     */
    @Query("SELECT s FROM DoctorSlot s " +
            "WHERE s.doctor.id = :doctorId " +
            "AND s.slotDate = :date " +
            "AND s.status = 'AVAILABLE' " +
            "ORDER BY s.startTime ASC")
    List<DoctorSlot> findAvailableSlotsByDoctorAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    /**
     * Find available slots in a date range (e.g., "next 7 days").
     */
    @Query("SELECT s FROM DoctorSlot s " +
            "WHERE s.doctor.id = :doctorId " +
            "AND s.slotDate BETWEEN :startDate AND :endDate " +
            "AND s.status = 'AVAILABLE' " +
            "ORDER BY s.slotDate ASC, s.startTime ASC")
    List<DoctorSlot> findAvailableSlotsInRange(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Get the next available slot for a doctor.
     */
    @Query("SELECT s FROM DoctorSlot s " +
            "WHERE s.doctor.id = :doctorId " +
            "AND s.status = 'AVAILABLE' " +
            "AND (s.slotDate > :today " +
            "     OR (s.slotDate = :today AND s.startTime > :currentTime)) " +
            "ORDER BY s.slotDate ASC, s.startTime ASC")
    List<DoctorSlot> findNextAvailableSlots(
            @Param("doctorId") Long doctorId,
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime,
            Pageable pageable);

    // =========================================================
    // CONCURRENCY-SAFE BOOKING (Phase 4 preview)
    // =========================================================

    /**
     * Lock a slot row for booking to prevent race conditions.
     * PESSIMISTIC_WRITE means: hold an exclusive DB lock until transaction ends.
     * Other transactions trying to read/update this row will WAIT.
     *
     * Used in Phase 4 booking flow. Listed here for reference.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DoctorSlot s WHERE s.id = :id")
    Optional<DoctorSlot> findByIdWithLock(@Param("id") Long id);

    // =========================================================
    // BULK OPERATIONS (used by holiday auto-cancel)
    // =========================================================

    /**
     * Bulk cancel all slots for a doctor on a specific date.
     * Used when a holiday is added for an existing date with slots.
     *
     * @Modifying tells Spring this is an UPDATE query
     * Returns number of rows affected.
     */
    @Modifying
    @Query("UPDATE DoctorSlot s SET " +
            "s.status = 'CANCELLED', " +
            "s.cancellationReason = :reason, " +
            "s.cancelledByUserId = :cancelledByUserId, " +
            "s.cancelledAt = :cancelledAt " +
            "WHERE s.doctor.id = :doctorId " +
            "AND s.slotDate = :date " +
            "AND s.status IN ('AVAILABLE', 'FULLY_BOOKED')")
    int bulkCancelSlotsForDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("reason") String reason,
            @Param("cancelledByUserId") Long cancelledByUserId,
            @Param("cancelledAt") LocalDateTime cancelledAt);

    /**
     * Bulk mark slots as EXPIRED when their date+time is in the past.
     * Used by a scheduled job (Phase 5).
     */
    @Modifying
    @Query("UPDATE DoctorSlot s SET s.status = 'EXPIRED' " +
            "WHERE s.status IN ('AVAILABLE', 'FULLY_BOOKED') " +
            "AND (s.slotDate < :today " +
            "     OR (s.slotDate = :today AND s.endTime <= :currentTime))")
    int bulkExpirePastSlots(
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime);

    // =========================================================
    // STATISTICS (admin dashboard)
    // =========================================================

    /**
     * Count slots by status for a doctor.
     */
    long countByDoctorAndStatus(Doctor doctor, DoctorSlotStatus status);

    /**
     * Count slots for a doctor in a date range.
     */
    long countByDoctorAndSlotDateBetween(
            Doctor doctor, LocalDate startDate, LocalDate endDate);

    /**
     * Get distinct slot dates for a doctor (for calendar UI).
     */
    @Query("SELECT DISTINCT s.slotDate FROM DoctorSlot s " +
            "WHERE s.doctor.id = :doctorId " +
            "AND s.slotDate >= :fromDate " +
            "AND s.status = 'AVAILABLE' " +
            "ORDER BY s.slotDate ASC")
    List<LocalDate> findDistinctAvailableDates(
            @Param("doctorId") Long doctorId,
            @Param("fromDate") LocalDate fromDate);
}