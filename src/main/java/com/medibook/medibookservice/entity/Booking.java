package com.medibook.medibookservice.entity;

import com.medibook.medibookservice.enums.BookingStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Represents a patient's reservation of a doctor's slot.
 *
 * <p>Lifecycle managed by {@link BookingStatus}:
 * BOOKED -> CANCELLED / COMPLETED / NO_SHOW (all terminal)</p>
 *
 * <p><b>Concurrency Control:</b></p>
 * <ul>
 *   <li>Optimistic locking via {@link #version} (auto-managed by JPA)</li>
 *   <li>Pessimistic locking on related DoctorSlot during creation (booking flow)</li>
 *   <li>UNIQUE constraint on (patient_user_id, slot_id) prevents double-booking</li>
 * </ul>
 *
 * <p><b>Booking Reference Format:</b> DR{doctorId}-{yyyyMMdd}-{sequence}<br/>
 * Example: DR5-20261209-001 (1st booking for Doctor 5 on Dec 9, 2026)</p>
 */
@Entity
@Table(name = "bookings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_booking_patient_slot",
                        columnNames = {"patient_user_id", "slot_id"}
                ),
                @UniqueConstraint(
                        name = "uq_booking_reference",
                        columnNames = {"booking_reference"}
                )
        },
        indexes = {
                @Index(name = "idx_booking_patient_status",
                        columnList = "patient_user_id, status"),
                @Index(name = "idx_booking_slot_status",
                        columnList = "slot_id, status"),
                @Index(name = "idx_booking_status_created",
                        columnList = "status, created_at"),
                @Index(name = "idx_booking_reference",
                        columnList = "booking_reference")
        })
@EntityListeners(AuditingEntityListener.class)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The patient who made this booking.
     * References the User entity (role = PATIENT).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_user_id", referencedColumnName = "id", nullable = false)
    private User patientUser;

    /**
     * The slot being booked.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", referencedColumnName = "id", nullable = false)
    private DoctorSlot slot;

    /**
     * Human-readable booking reference (e.g., "DR5-20261209-001").
     * Computed at creation time from doctor + slot_date + sequence.
     */
    @Column(name = "booking_reference", nullable = false, length = 30, unique = true)
    private String bookingReference;

    /**
     * Current lifecycle state of this booking.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status = BookingStatus.BOOKED;

    /**
     * Optional notes from the patient (e.g., symptoms, reason for visit).
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Reason for cancellation (populated when status = CANCELLED).
     */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    /**
     * ID of user who cancelled this booking (audit trail).
     * Can be patient_user_id, doctor's user_id, or admin's user_id.
     */
    @Column(name = "cancelled_by_user_id")
    private Long cancelledByUserId;

    /**
     * Timestamp when booking was cancelled.
     */
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * Timestamp when booking was marked completed by doctor.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Version for optimistic locking.
     * JPA automatically increments on every update.
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

    public Booking() {
    }

    public Booking(User patientUser, DoctorSlot slot, String bookingReference) {
        this.patientUser = patientUser;
        this.slot = slot;
        this.bookingReference = bookingReference;
        this.status = BookingStatus.BOOKED;
    }

    // =========================================================
    // BUSINESS HELPER METHODS
    // =========================================================

    /**
     * Returns true if this booking can still be cancelled (not in terminal state).
     */
    public boolean isCancellable() {
        return status == BookingStatus.BOOKED;
    }

    /**
     * Returns true if this booking is in a terminal state.
     */
    public boolean isTerminal() {
        return status != BookingStatus.BOOKED;
    }

    /**
     * Returns the slot's date+time as a single LocalDateTime.
     */
    public LocalDateTime getSlotDateTime() {
        if (slot == null) return null;
        return LocalDateTime.of(slot.getSlotDate(), slot.getStartTime());
    }

    /**
     * Returns true if the slot's start time has passed.
     */
    public boolean isPastSlotTime() {
        LocalDateTime slotTime = getSlotDateTime();
        return slotTime != null && slotTime.isBefore(LocalDateTime.now());
    }

    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatientUser() { return patientUser; }
    public void setPatientUser(User patientUser) { this.patientUser = patientUser; }

    public DoctorSlot getSlot() { return slot; }
    public void setSlot(DoctorSlot slot) { this.slot = slot; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

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

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}