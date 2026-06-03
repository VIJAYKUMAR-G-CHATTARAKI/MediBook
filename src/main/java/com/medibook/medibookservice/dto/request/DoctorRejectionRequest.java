package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for admin rejecting a doctor.
 * Used in PUT /admin/doctors/{id}/reject (ADMIN role).
 * Reason is REQUIRED so doctor knows what to fix.
 */
public class DoctorRejectionRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 10, max = 1000,
            message = "Rejection reason must be between 10 and 1000 characters")
    private String rejectionReason;

    /**
     * Whether the doctor is allowed to update their profile and resubmit.
     * If false, rejection is final (must contact admin to re-enable).
     */
    @NotNull(message = "Resubmission allowed flag is required")
    private Boolean resubmissionAllowed;

    public DoctorRejectionRequest() {}

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Boolean getResubmissionAllowed() { return resubmissionAllowed; }
    public void setResubmissionAllowed(Boolean resubmissionAllowed) { this.resubmissionAllowed = resubmissionAllowed; }
}