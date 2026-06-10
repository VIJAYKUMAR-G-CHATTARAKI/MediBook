package com.medibook.medibookservice.entity;

import com.medibook.medibookservice.enums.BookingHistoryAction;
import com.medibook.medibookservice.enums.BookingStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Immutable audit trail entry for a Booking's state transitions.
 *
 * <p>Each row captures ONE event - creation, cancellation, completion, etc.
 * Rows are append-only: never updated, never deleted.</p>
 *
 * <p><b>Rich Audit Trail:</b> Captures old/new status, who performed the action,
 * when, and optional notes (e.g., cancellation reason).</p>
 *
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>Compliance: Who cancelled and when?</li>
 *   <li>Analytics: How many no-shows per doctor per month?</li>
 *   <li>Debugging: What's the complete history of booking X?</li>
 *   <li>Forensics: Detect unusual cancellation patterns</li>
 * </ul>
 */
@Entity
@Table(name = "booking_history",
        indexes = {
                @Index(name = "idx_history_booking_time",
                        columnList = "booking_id, performed_at"),
                @Index(name = "idx_history_performer",
                        columnList = "performed_by_user_id"),
                @Index(name = "idx_history_action_time",
                        columnList = "action, performed_at")
        })
public class BookingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The booking this history entry belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", referencedColumnName = "id", nullable = false)
    private Booking booking;

    /**
     * Status BEFORE this event.
     * Null when action = CREATED (booking didn't exist before).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 20)
    private BookingStatus oldStatus;

    /**
     * Status AFTER this event.
     * For CREATED action: BOOKED.
     * For CANCELLED: CANCELLED. Etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private BookingStatus newStatus;

    /**
     * What action caused this state transition.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private BookingHistoryAction action;

    /**
     * User who performed the action.
     * Can be patient, doctor, or admin (all are Users).
     */
    @Column(name = "performed_by_user_id", nullable = false)
    private Long performedByUserId;

    /**
     * Optional notes about the action.
     * For cancellations: typically the cancellation reason.
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Timestamp when the action was performed.
     * Set explicitly by the service (not auto-generated) for accuracy.
     */
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public BookingHistory() {
    }

    public BookingHistory(Booking booking,
                          BookingStatus oldStatus,
                          BookingStatus newStatus,
                          BookingHistoryAction action,
                          Long performedByUserId,
                          String notes) {
        this.booking = booking;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.action = action;
        this.performedByUserId = performedByUserId;
        this.notes = notes;
        this.performedAt = LocalDateTime.now();
    }

    // =========================================================
    // STATIC FACTORY METHODS (clear intent at call site)
    // =========================================================

    /**
     * Factory for the CREATED event (initial booking).
     */
    public static BookingHistory created(Booking booking, Long performedByUserId) {
        return new BookingHistory(
                booking,
                null,                                  // no previous status
                BookingStatus.BOOKED,
                BookingHistoryAction.CREATED,
                performedByUserId,
                null                                   // no notes for creation
        );
    }

    /**
     * Factory for the CANCELLED event.
     */
    public static BookingHistory cancelled(Booking booking,
                                           BookingStatus oldStatus,
                                           Long performedByUserId,
                                           String reason) {
        return new BookingHistory(
                booking,
                oldStatus,
                BookingStatus.CANCELLED,
                BookingHistoryAction.CANCELLED,
                performedByUserId,
                reason
        );
    }

    /**
     * Factory for the COMPLETED event.
     */
    public static BookingHistory completed(Booking booking,
                                           BookingStatus oldStatus,
                                           Long performedByUserId) {
        return new BookingHistory(
                booking,
                oldStatus,
                BookingStatus.COMPLETED,
                BookingHistoryAction.COMPLETED,
                performedByUserId,
                null
        );
    }

    /**
     * Factory for the NO_SHOW_MARKED event.
     */
    public static BookingHistory noShowMarked(Booking booking,
                                              BookingStatus oldStatus,
                                              Long performedByUserId) {
        return new BookingHistory(
                booking,
                oldStatus,
                BookingStatus.NO_SHOW,
                BookingHistoryAction.NO_SHOW_MARKED,
                performedByUserId,
                null
        );
    }

    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public BookingStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(BookingStatus oldStatus) { this.oldStatus = oldStatus; }

    public BookingStatus getNewStatus() { return newStatus; }
    public void setNewStatus(BookingStatus newStatus) { this.newStatus = newStatus; }

    public BookingHistoryAction getAction() { return action; }
    public void setAction(BookingHistoryAction action) { this.action = action; }

    public Long getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(Long performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }
}