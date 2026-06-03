package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Request payload for admin approving a doctor.
 * Used in PUT /admin/doctors/{id}/approve (ADMIN role).
 * Notes field is optional.
 */
public class DoctorApprovalRequest {

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    public DoctorApprovalRequest() {}

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}