package com.medibook.medibookservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Request to create/update a doctor's slot configuration.
 * Used in POST/PUT /admin/doctors/{id}/slot-config
 */
public class SlotConfigRequest {

    @NotNull(message = "Slot duration is required")
    @Min(value = 5, message = "Slot duration must be at least 5 minutes")
    @Max(value = 240, message = "Slot duration cannot exceed 4 hours")
    private Integer slotDurationMinutes;

    @NotNull(message = "Max bookings per slot is required")
    @Min(value = 1, message = "Max bookings must be at least 1")
    @Max(value = 50, message = "Max bookings cannot exceed 50")
    private Integer maxBookingsPerSlot;

    @NotBlank(message = "Timezone is required")
    @Size(max = 50)
    private String timezone;

    private Boolean active;

    /**
     * 7 entries (one per day) - or empty to use defaults.
     */
    @Valid
    @Size(max = 7, message = "Cannot have more than 7 weekly schedule entries")
    private List<WeeklyScheduleRequest> weeklySchedules;

    public SlotConfigRequest() {}

    // Getters and setters
    public Integer getSlotDurationMinutes() { return slotDurationMinutes; }
    public void setSlotDurationMinutes(Integer slotDurationMinutes) {
        this.slotDurationMinutes = slotDurationMinutes;
    }

    public Integer getMaxBookingsPerSlot() { return maxBookingsPerSlot; }
    public void setMaxBookingsPerSlot(Integer maxBookingsPerSlot) {
        this.maxBookingsPerSlot = maxBookingsPerSlot;
    }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public List<WeeklyScheduleRequest> getWeeklySchedules() { return weeklySchedules; }
    public void setWeeklySchedules(List<WeeklyScheduleRequest> weeklySchedules) {
        this.weeklySchedules = weeklySchedules;
    }
}