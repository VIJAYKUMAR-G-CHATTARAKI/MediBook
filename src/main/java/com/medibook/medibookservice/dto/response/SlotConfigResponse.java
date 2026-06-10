package com.medibook.medibookservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Slot configuration response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlotConfigResponse {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private Integer slotDurationMinutes;
    private Integer maxBookingsPerSlot;
    private String timezone;
    private Boolean active;
    private List<WeeklyScheduleResponse> weeklySchedules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SlotConfigResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

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

    public List<WeeklyScheduleResponse> getWeeklySchedules() { return weeklySchedules; }
    public void setWeeklySchedules(List<WeeklyScheduleResponse> weeklySchedules) {
        this.weeklySchedules = weeklySchedules;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}