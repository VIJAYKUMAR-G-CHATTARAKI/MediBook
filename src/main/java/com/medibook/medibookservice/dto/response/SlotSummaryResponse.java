package com.medibook.medibookservice.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Minimal slot data for public listings.
 * No status (already filtered to AVAILABLE), no audit info.
 */
public class SlotSummaryResponse {

    private Long id;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer remainingSpots;

    public SlotSummaryResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Integer getRemainingSpots() { return remainingSpots; }
    public void setRemainingSpots(Integer remainingSpots) { this.remainingSpots = remainingSpots; }
}