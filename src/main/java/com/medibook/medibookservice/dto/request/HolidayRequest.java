package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request to add a single holiday for a doctor.
 */
public class HolidayRequest {

    @NotNull(message = "Holiday date is required")
    private LocalDate holidayDate;

    @NotBlank(message = "Reason is required")
    @Size(max = 200, message = "Reason cannot exceed 200 characters")
    private String reason;

    /**
     * If true, also cancels existing slots for this date.
     * Defaults to true (recommended behavior).
     */
    private Boolean cancelExistingSlots = true;

    public HolidayRequest() {}

    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getCancelExistingSlots() { return cancelExistingSlots; }
    public void setCancelExistingSlots(Boolean cancelExistingSlots) {
        this.cancelExistingSlots = cancelExistingSlots;
    }
}