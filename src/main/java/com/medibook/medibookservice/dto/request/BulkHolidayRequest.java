package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Request to add multiple holidays at once (e.g., vacation week).
 */
public class BulkHolidayRequest {

    @NotEmpty(message = "At least one date is required")
    @Size(max = 365, message = "Cannot add more than 365 holidays at once")
    private List<LocalDate> holidayDates;

    @NotBlank(message = "Reason is required")
    @Size(max = 200, message = "Reason cannot exceed 200 characters")
    private String reason;

    private Boolean cancelExistingSlots = true;

    public BulkHolidayRequest() {}

    public List<LocalDate> getHolidayDates() { return holidayDates; }
    public void setHolidayDates(List<LocalDate> holidayDates) { this.holidayDates = holidayDates; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getCancelExistingSlots() { return cancelExistingSlots; }
    public void setCancelExistingSlots(Boolean cancelExistingSlots) {
        this.cancelExistingSlots = cancelExistingSlots;
    }
}