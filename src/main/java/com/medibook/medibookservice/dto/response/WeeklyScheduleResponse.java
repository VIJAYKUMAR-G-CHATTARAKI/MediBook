package com.medibook.medibookservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Per-day schedule response (nested in SlotConfigResponse).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeeklyScheduleResponse {

    private Long id;
    private DayOfWeek dayOfWeek;
    private Boolean isWorkingDay;
    private LocalTime workStartTime;
    private LocalTime workEndTime;

    public WeeklyScheduleResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Boolean getIsWorkingDay() { return isWorkingDay; }
    public void setIsWorkingDay(Boolean isWorkingDay) { this.isWorkingDay = isWorkingDay; }

    public LocalTime getWorkStartTime() { return workStartTime; }
    public void setWorkStartTime(LocalTime workStartTime) { this.workStartTime = workStartTime; }

    public LocalTime getWorkEndTime() { return workEndTime; }
    public void setWorkEndTime(LocalTime workEndTime) { this.workEndTime = workEndTime; }
}