package com.medibook.medibookservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medibook.medibookservice.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Full booking details for authenticated views.
 * Used by patient, doctor, and admin endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingResponse {

    // Booking identifiers
    private Long id;
    private String bookingReference;
    private BookingStatus status;

    // Patient info (denormalized for display convenience)
    private Long patientUserId;
    private String patientName;
    private String patientEmail;

    // Slot info (denormalized)
    private Long slotId;
    private LocalDate slotDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;

    // Doctor info (denormalized)
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;

    // Patient-provided notes
    private String notes;

    // Cancellation audit (only set if status = CANCELLED)
    private String cancellationReason;
    private Long cancelledByUserId;
    private LocalDateTime cancelledAt;

    // Completion timestamp (only set if status = COMPLETED)
    private LocalDateTime completedAt;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingResponse() {}

    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public Long getPatientUserId() { return patientUserId; }
    public void setPatientUserId(Long patientUserId) { this.patientUserId = patientUserId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getSlotStartTime() { return slotStartTime; }
    public void setSlotStartTime(LocalTime slotStartTime) { this.slotStartTime = slotStartTime; }

    public LocalTime getSlotEndTime() { return slotEndTime; }
    public void setSlotEndTime(LocalTime slotEndTime) { this.slotEndTime = slotEndTime; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorSpecialization() { return doctorSpecialization; }
    public void setDoctorSpecialization(String doctorSpecialization) {
        this.doctorSpecialization = doctorSpecialization;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Long getCancelledByUserId() { return cancelledByUserId; }
    public void setCancelledByUserId(Long cancelledByUserId) {
        this.cancelledByUserId = cancelledByUserId;
    }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}