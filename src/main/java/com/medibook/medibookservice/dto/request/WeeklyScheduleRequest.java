package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Per-day schedule entry within a slot config request.
 * Used as a nested DTO inside {@link SlotConfigRequest}.
 */
public class WeeklyScheduleRequest {

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "isWorkingDay flag is required")
    private Boolean isWorkingDay;

    /**
     * Required when isWorkingDay is true.
     * Should be null when isWorkingDay is false.
     */
    private LocalTime workStartTime;

    /**
     * Required when isWorkingDay is true.
     * Should be null when isWorkingDay is false.
     */
    private LocalTime workEndTime;

    public WeeklyScheduleRequest() {}

    // Getters and setters
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Boolean getIsWorkingDay() { return isWorkingDay; }
    public void setIsWorkingDay(Boolean isWorkingDay) { this.isWorkingDay = isWorkingDay; }

    public LocalTime getWorkStartTime() { return workStartTime; }
    public void setWorkStartTime(LocalTime workStartTime) { this.workStartTime = workStartTime; }

    public LocalTime getWorkEndTime() { return workEndTime; }
    public void setWorkEndTime(LocalTime workEndTime) { this.workEndTime = workEndTime; }
}