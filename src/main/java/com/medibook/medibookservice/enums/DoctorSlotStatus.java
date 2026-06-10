package com.medibook.medibookservice.enums;

/**
 * Represents the lifecycle states of a Doctor's appointment slot.
 *
 * <p><b>State Transitions:</b></p>
 * <ul>
 *   <li>{@link #AVAILABLE} → {@link #FULLY_BOOKED} (when max bookings reached)</li>
 *   <li>{@link #AVAILABLE} → {@link #CANCELLED} (admin/doctor cancels)</li>
 *   <li>{@link #AVAILABLE} → {@link #EXPIRED} (time passes, scheduled job)</li>
 *   <li>{@link #FULLY_BOOKED} → {@link #CANCELLED} (rare: admin cancels with bookings)</li>
 *   <li>{@link #FULLY_BOOKED} → {@link #EXPIRED} (time passes)</li>
 * </ul>
 *
 * <p><b>Booking Rules:</b></p>
 * <ul>
 *   <li>Only {@link #AVAILABLE} slots can accept new bookings</li>
 *   <li>Public listings only show {@link #AVAILABLE} slots</li>
 * </ul>
 */
public enum DoctorSlotStatus {

    /**
     * Slot has space for bookings (current_bookings < max_bookings).
     * This is the only state where patients can book.
     */
    AVAILABLE,

    /**
     * Slot has reached its maximum booking capacity.
     * No new bookings allowed, but existing bookings remain valid.
     */
    FULLY_BOOKED,

    /**
     * Slot was cancelled by doctor or admin.
     * Reason is captured in cancellation_reason field.
     * Existing bookings (if any) need to be handled separately.
     */
    CANCELLED,

    /**
     * Slot's date/time has passed.
     * Updated by a scheduled job that runs periodically.
     * Cannot accept new bookings.
     */
    EXPIRED
}