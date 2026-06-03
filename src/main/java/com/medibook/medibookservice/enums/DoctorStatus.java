package com.medibook.medibookservice.enums;

/**
 * Represents the lifecycle states of a Doctor's profile approval process.
 *
 * <p><b>Workflow:</b></p>
 * <ol>
 *   <li>Doctor registers (User account) and creates profile → {@link #PENDING_APPROVAL}</li>
 *   <li>Admin reviews → {@link #APPROVED} or {@link #REJECTED}</li>
 *   <li>If REJECTED with resubmission allowed, doctor can update → {@link #PENDING_APPROVAL}</li>
 *   <li>If APPROVED, admin can later → {@link #SUSPENDED} (temporary disable)</li>
 *   <li>SUSPENDED can be reactivated → {@link #APPROVED}</li>
 * </ol>
 *
 * <p><b>Visibility Rules:</b></p>
 * <ul>
 *   <li>Only {@link #APPROVED} doctors appear in public listings</li>
 *   <li>Only {@link #APPROVED} doctors can receive bookings</li>
 * </ul>
 */
public enum DoctorStatus {

    /**
     * Doctor has submitted their profile and is awaiting admin review.
     * This is the initial state for any new doctor profile.
     */
    PENDING_APPROVAL,

    /**
     * Admin has approved the doctor's profile.
     * Doctor is now publicly visible and can receive appointment bookings.
     */
    APPROVED,

    /**
     * Admin has rejected the doctor's profile.
     * A rejection_reason will be provided.
     * Doctor may resubmit only if resubmission_allowed flag is true.
     */
    REJECTED,

    /**
     * Admin has temporarily disabled an approved doctor.
     * Profile exists but is not publicly visible or bookable.
     * Can be reactivated to APPROVED by admin.
     */
    SUSPENDED
}