package com.medibook.medibookservice.entity;

import com.medibook.medibookservice.enums.DoctorSlotStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents a bookable appointment slot for a doctor.
 *
 * <p>Each slot is a specific time window on a specific date for a specific doctor.
 * Generated based on {@link DoctorSlotConfig} and {@link DoctorWeeklySchedule},
 * skipping dates that are {@link DoctorHoliday}s.</p>
 *
 * <p>Lifecycle managed by {@link DoctorSlotStatus}:
 * AVAILABLE -> FULLY_BOOKED (max reached) / CANCELLED (admin) / EXPIRED (time passed)</p>
 *
 * <p><b>Concurrency:</b> Uses optimistic locking via @Version to prevent
 * double-booking when multiple patients try the same slot simultaneously (Phase 4).</p>
 */
@Entity
@Table(name = "doctor_slots",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_slot_doctor_date_time",
                        columnNames = {"doctor_id", "slot_date", "start_time"}
                )
        },
        indexes = {
                @Index(name = "idx_slot_doctor_date",
                        columnList = "doctor_id, slot_date"),
                @Index(name = "idx_slot_date_status",
                        columnList = "slot_date, status"),
                @Index(name = "idx_slot_status", columnList = "status")
        })
@EntityListeners(AuditingEntityListener.class)
public class DoctorSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The doctor this slot belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", referencedColumnName = "id", nullable = false)
    private Doctor doctor;

    /**
     * Date of this slot (e.g., 2026-12-09).
     */
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    /**
     * Start time of this slot (e.g., 10:00).
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * End time of this slot (e.g., 10:30 = start + slot_duration).
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Maximum bookings allowed for this slot.
     * Defaults to value from DoctorSlotConfig, but can be overridden per slot.
     */
    @Column(name = "max_bookings", nullable = false)
    private Integer maxBookings = 2;

    /**
     * Current number of confirmed bookings on this slot.
     * Updated atomically when bookings are created/cancelled.
     * Denormalized for performance - avoids counting bookings table on every read.
     */
    @Column(name = "current_bookings", nullable = false)
    private Integer currentBookings = 0;

    /**
     * Current lifecycle state of this slot.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DoctorSlotStatus status = DoctorSlotStatus.AVAILABLE;

    /**
     * Reason for cancellation (populated when status=CANCELLED).
     */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /**
     * ID of user who cancelled this slot (audit trail).
     */
    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    /**
     * Timestamp when slot was cancelled.
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Version for optimistic locking.
     * Used in Phase 4 to prevent double-booking concurrency issues.
     * JPA automatically increments this on every update.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public DoctorSlot() {
    }

    public DoctorSlot(Doctor doctor, LocalDate slotDate,
                      LocalTime startTime, LocalTime endTime,
                      Integer maxBookings) {
        this.doctor = doctor;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxBookings = maxBookings;
        this.currentBookings = 0;
        this.status = DoctorSlotStatus.AVAILABLE;
    }

    // =========================================================
    // BUSINESS HELPER METHODS
    // =========================================================

    /**
     * Returns true if this slot can accept new bookings.
     */
    public boolean isBookable() {
        return status == DoctorSlotStatus.AVAILABLE
                && currentBookings < maxBookings;
    }

    /**
     * Returns true if this slot has reached max capacity.
     */
    public boolean isFull() {
        return currentBookings >= maxBookings;
    }

    /**
     * Returns the number of remaining booking spots.
     */
    public Integer getRemainingSpots() {
        return Math.max(0, maxBookings - currentBookings);
    }

    /**
     * Returns true if this slot's date+time is in the past.
     */
    public boolean isExpired() {
        LocalDateTime slotDateTime = LocalDateTime.of(slotDate, endTime);
        return slotDateTime.isBefore(LocalDateTime.now());
    }

    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Integer getMaxBookings() { return maxBookings; }
    public void setMaxBookings(Integer maxBookings) { this.maxBookings = maxBookings; }

    public Integer getCurrentBookings() { return currentBookings; }
    public void setCurrentBookings(Integer currentBookings) {
        this.currentBookings = currentBookings;
    }

    public DoctorSlotStatus getStatus() { return status; }
    public void setStatus(DoctorSlotStatus status) { this.status = status; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Long getCancelledByUserId() { return cancelledByUserId; }
    public void setCancelledByUserId(Long cancelledByUserId) {
        this.cancelledByUserId = cancelledByUserId;
    }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}