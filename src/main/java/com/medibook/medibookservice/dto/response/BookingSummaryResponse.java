package com.medibook.medibookservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.medibook.medibookservice.enums.BookingStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Compact booking representation for list views.
 * Less data per row = faster lists, smaller JSON.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookingSummaryResponse {

    private Long id;
    private String bookingReference;
    private BookingStatus status;
    private LocalDate slotDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private String doctorName;
    private String patientName;

    public BookingSummaryResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getSlotStartTime() { return slotStartTime; }
    public void setSlotStartTime(LocalTime slotStartTime) { this.slotStartTime = slotStartTime; }

    public LocalTime getSlotEndTime() { return slotEndTime; }
    public void setSlotEndTime(LocalTime slotEndTime) { this.slotEndTime = slotEndTime; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
}