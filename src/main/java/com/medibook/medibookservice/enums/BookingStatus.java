package com.medibook.medibookservice.enums;

/**
 * Represents the lifecycle states of a patient booking.
 *
 * <p><b>State Transitions:</b></p>
 * <ul>
 *   <li>{@link #BOOKED} → {@link #CANCELLED} (patient/doctor/admin cancels)</li>
 *   <li>{@link #BOOKED} → {@link #COMPLETED} (doctor marks complete after visit)</li>
 *   <li>{@link #BOOKED} → {@link #NO_SHOW} (doctor marks after slot expired)</li>
 * </ul>
 *
 * <p><b>Terminal States:</b> CANCELLED, COMPLETED, NO_SHOW cannot transition further.</p>
 *
 * <p><b>Business Rules:</b></p>
 * <ul>
 *   <li>Only BOOKED bookings can be cancelled</li>
 *   <li>COMPLETED requires the slot's end time has passed</li>
 *   <li>NO_SHOW requires the slot's end time has passed</li>
 *   <li>Cancellation must be at least 2 hours before slot time</li>
 * </ul>
 */
public enum BookingStatus {

    /**
     * Active booking - patient has reserved a slot, awaiting attendance.
     * This is the initial state when a booking is created.
     */
    BOOKED,

    /**
     * Patient attended and doctor completed the consultation.
     * Terminal state - cannot transition further.
     */
    COMPLETED,

    /**
     * Booking was cancelled (by patient, doctor, or admin).
     * Cancellation reason and audit trail captured.
     * Terminal state - cannot transition further.
     */
    CANCELLED,

    /**
     * Patient did not attend the booked slot.
     * Marked by doctor after slot's end time has passed.
     * Used for analytics, no-show tracking, and potential penalties.
     * Terminal state - cannot transition further.
     */
    NO_SHOW
}