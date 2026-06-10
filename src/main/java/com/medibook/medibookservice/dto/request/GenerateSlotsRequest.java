package com.medibook.medibookservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request to generate slots for a doctor over a date range.
 */
public class GenerateSlotsRequest {

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    /**
     * If true, skip dates where slots already exist (don't overwrite).
     * If false, error out if any slot already exists in range.
     * Defaults to true.
     */
    private Boolean skipExisting = true;

    public GenerateSlotsRequest() {}

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Boolean getSkipExisting() { return skipExisting; }
    public void setSkipExisting(Boolean skipExisting) { this.skipExisting = skipExisting; }
}