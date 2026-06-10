package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request from doctor to mark a booking as COMPLETED.
 * Optional notes (consultation summary).
 */
public class CompleteBookingRequest {

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    public CompleteBookingRequest() {}

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}