package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to cancel a booking.
 * Reason is required for audit trail.
 */
public class CancelBookingRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;

    public CancelBookingRequest() {}

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}