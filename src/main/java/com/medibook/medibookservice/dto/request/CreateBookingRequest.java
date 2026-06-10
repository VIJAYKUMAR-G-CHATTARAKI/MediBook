package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new booking.
 * Sent by patient when reserving a slot.
 */
public class CreateBookingRequest {

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    /**
     * Optional notes from patient (symptoms, reason for visit, etc.)
     */
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    public CreateBookingRequest() {}

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}