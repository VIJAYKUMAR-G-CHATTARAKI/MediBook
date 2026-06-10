package com.medibook.medibookservice.enums;

/**
 * Represents the type of action that occurred on a Booking,
 * recorded in the BookingHistory audit trail.
 *
 * <p>While {@link BookingStatus} tracks the current STATE of a booking,
 * BookingHistoryAction tracks the EVENT that caused a state change.</p>
 *
 * <p><b>Typical history sequence:</b></p>
 * <ul>
 *   <li>CREATED → (booking starts as BOOKED)</li>
 *   <li>CANCELLED → (booking moves to CANCELLED)</li>
 *   <li>COMPLETED → (booking moves to COMPLETED)</li>
 *   <li>NO_SHOW_MARKED → (booking moves to NO_SHOW)</li>
 * </ul>
 *
 * <p>Each history row also captures: old status, new status, performer, timestamp, notes.</p>
 */
public enum BookingHistoryAction {

    /**
     * Booking was created (patient first booked the slot).
     * Always paired with new_status = BOOKED, old_status = null.
     */
    CREATED,

    /**
     * Booking was cancelled.
     * Paired with new_status = CANCELLED.
     * Cancellation reason should be captured in BookingHistory.notes.
     */
    CANCELLED,

    /**
     * Booking was marked completed by the doctor after the consultation.
     * Paired with new_status = COMPLETED.
     */
    COMPLETED,

    /**
     * Booking was marked as no-show by the doctor (patient didn't attend).
     * Paired with new_status = NO_SHOW.
     */
    NO_SHOW_MARKED
}