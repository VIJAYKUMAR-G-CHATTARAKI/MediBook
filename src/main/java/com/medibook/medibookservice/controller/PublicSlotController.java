package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.SlotSummaryResponse;
import com.medibook.medibookservice.service.DoctorSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Public endpoints for patients browsing doctor availability.
 * No authentication required.
 */
@RestController
@RequestMapping("/doctors")
public class PublicSlotController {

    private final DoctorSlotService slotService;

    public PublicSlotController(DoctorSlotService slotService) {
        this.slotService = slotService;
    }

    /**
     * Get AVAILABLE slots for a doctor on a specific date.
     *
     * Example: GET /doctors/5/slots/available?date=2026-12-09
     */
    @GetMapping("/{doctorId}/slots/available")
    public ResponseEntity<ApiResponse<List<SlotSummaryResponse>>> getAvailableSlotsByDate(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<SlotSummaryResponse> slots = slotService.getAvailableSlotsByDoctorAndDate(
                doctorId, date);
        return ResponseEntity.ok(ApiResponse.success("Available slots retrieved", slots));
    }

    /**
     * Get AVAILABLE slots for a doctor in a date range (max 90 days).
     *
     * Example: GET /doctors/5/slots/available/range?startDate=2026-12-01&endDate=2026-12-15
     */
    @GetMapping("/{doctorId}/slots/available/range")
    public ResponseEntity<ApiResponse<List<SlotSummaryResponse>>> getAvailableSlotsInRange(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<SlotSummaryResponse> slots = slotService.getAvailableSlotsInRange(
                doctorId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Available slots retrieved", slots));
    }

    /**
     * Get the next N available slots for a doctor.
     *
     * Example: GET /doctors/5/slots/available/next?limit=5
     */
    @GetMapping("/{doctorId}/slots/available/next")
    public ResponseEntity<ApiResponse<List<SlotSummaryResponse>>> getNextAvailableSlots(
            @PathVariable Long doctorId,
            @RequestParam(defaultValue = "5") int limit) {

        List<SlotSummaryResponse> slots = slotService.getNextAvailableSlots(doctorId, limit);
        return ResponseEntity.ok(ApiResponse.success("Next available slots retrieved", slots));
    }

    /**
     * Get distinct dates with availability (for calendar UI).
     *
     * Example: GET /doctors/5/slots/dates?fromDate=2026-12-01
     */
    @GetMapping("/{doctorId}/slots/dates")
    public ResponseEntity<ApiResponse<List<LocalDate>>> getDatesWithAvailability(
            @PathVariable Long doctorId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {

        LocalDate from = (fromDate != null) ? fromDate : LocalDate.now();
        List<LocalDate> dates = slotService.getDatesWithAvailability(doctorId, from);
        return ResponseEntity.ok(ApiResponse.success("Available dates retrieved", dates));
    }
}