package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.request.*;
import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.HolidayResponse;
import com.medibook.medibookservice.dto.response.SlotConfigResponse;
import com.medibook.medibookservice.dto.response.SlotResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.service.DoctorHolidayService;
import com.medibook.medibookservice.service.DoctorSlotConfigService;
import com.medibook.medibookservice.service.DoctorSlotService;
import com.medibook.medibookservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Admin endpoints for managing slot configs, holidays, generation, and slot cancellation.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSlotController {

    private static final Logger log = LoggerFactory.getLogger(AdminSlotController.class);

    private final DoctorSlotConfigService slotConfigService;
    private final DoctorHolidayService holidayService;
    private final DoctorSlotService slotService;
    private final UserService userService;

    public AdminSlotController(DoctorSlotConfigService slotConfigService,
                               DoctorHolidayService holidayService,
                               DoctorSlotService slotService,
                               UserService userService) {
        this.slotConfigService = slotConfigService;
        this.holidayService = holidayService;
        this.slotService = slotService;
        this.userService = userService;
    }

    // =========================================================
    // SLOT CONFIG MANAGEMENT
    // =========================================================

    @PostMapping("/doctors/{doctorId}/slot-config")
    public ResponseEntity<ApiResponse<SlotConfigResponse>> createOrUpdateSlotConfig(
            @PathVariable Long doctorId,
            @Valid @RequestBody SlotConfigRequest request) {

        log.info("Admin updating slot config for doctor {}", doctorId);
        SlotConfigResponse response = slotConfigService.createOrUpdateConfig(doctorId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Slot config saved", response));
    }

    @GetMapping("/doctors/{doctorId}/slot-config")
    public ResponseEntity<ApiResponse<SlotConfigResponse>> getSlotConfig(
            @PathVariable Long doctorId) {

        SlotConfigResponse response = slotConfigService.getConfigByDoctorId(doctorId);
        return ResponseEntity.ok(
                ApiResponse.success("Slot config retrieved", response));
    }

    @PutMapping("/doctors/{doctorId}/slot-config/active")
    public ResponseEntity<ApiResponse<SlotConfigResponse>> setConfigActive(
            @PathVariable Long doctorId,
            @RequestParam boolean active) {

        log.info("Admin setting config active={} for doctor {}", active, doctorId);
        SlotConfigResponse response = slotConfigService.setConfigActive(doctorId, active);
        return ResponseEntity.ok(
                ApiResponse.success("Config active state updated", response));
    }

    // =========================================================
    // SLOT GENERATION
    // =========================================================

    @PostMapping("/doctors/{doctorId}/slots/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateSlots(
            @PathVariable Long doctorId,
            @Valid @RequestBody GenerateSlotsRequest request) {

        log.info("Admin generating slots for doctor {}", doctorId);
        int count = slotConfigService.generateSlots(doctorId, request);
        Map<String, Object> data = Map.of(
                "doctorId", doctorId,
                "slotsGenerated", count,
                "startDate", request.getStartDate(),
                "endDate", request.getEndDate()
        );
        return new ResponseEntity<>(
                ApiResponse.success("Slot generation complete", data),
                HttpStatus.CREATED);
    }

    // =========================================================
    // HOLIDAY MANAGEMENT
    // =========================================================

    @PostMapping("/doctors/{doctorId}/holidays")
    public ResponseEntity<ApiResponse<HolidayResponse>> addHoliday(
            @PathVariable Long doctorId,
            @Valid @RequestBody HolidayRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = userService.findByEmail(userDetails.getUsername());
        log.info("Admin {} adding holiday for doctor {}", admin.getEmail(), doctorId);

        HolidayResponse response = holidayService.addHoliday(doctorId, request, admin.getId());
        return new ResponseEntity<>(
                ApiResponse.success("Holiday added", response),
                HttpStatus.CREATED);
    }

    @PostMapping("/doctors/{doctorId}/holidays/bulk")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> addBulkHolidays(
            @PathVariable Long doctorId,
            @Valid @RequestBody BulkHolidayRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = userService.findByEmail(userDetails.getUsername());
        log.info("Admin {} bulk-adding {} holidays for doctor {}",
                admin.getEmail(), request.getHolidayDates().size(), doctorId);

        List<HolidayResponse> response = holidayService.addBulkHolidays(
                doctorId, request, admin.getId());
        return new ResponseEntity<>(
                ApiResponse.success("Holidays added", response),
                HttpStatus.CREATED);
    }

    @GetMapping("/doctors/{doctorId}/holidays")
    public ResponseEntity<ApiResponse<Page<HolidayResponse>>> getHolidays(
            @PathVariable Long doctorId,
            @PageableDefault(size = 20, sort = "holidayDate",
                    direction = Sort.Direction.ASC) Pageable pageable) {

        Page<HolidayResponse> holidays = holidayService.getHolidaysForDoctor(doctorId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Holidays retrieved", holidays));
    }

    @GetMapping("/doctors/{doctorId}/holidays/upcoming")
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getUpcomingHolidays(
            @PathVariable Long doctorId) {

        List<HolidayResponse> holidays = holidayService.getUpcomingHolidays(doctorId);
        return ResponseEntity.ok(
                ApiResponse.success("Upcoming holidays retrieved", holidays));
    }

    @DeleteMapping("/holidays/{holidayId}")
    public ResponseEntity<ApiResponse<Void>> removeHoliday(@PathVariable Long holidayId) {
        log.info("Admin removing holiday {}", holidayId);
        holidayService.removeHoliday(holidayId);
        return ResponseEntity.ok(
                ApiResponse.success("Holiday removed", null));
    }

    // =========================================================
    // SLOT VIEWING & CANCELLATION
    // =========================================================

    @GetMapping("/doctors/{doctorId}/slots")
    public ResponseEntity<ApiResponse<Page<SlotResponse>>> getDoctorSlots(
            @PathVariable Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "slotDate",
                    direction = Sort.Direction.ASC) Pageable pageable) {

        Page<SlotResponse> slots = slotService.getSlotsForDoctorInRange(
                doctorId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Slots retrieved", slots));
    }

    @PostMapping("/slots/{slotId}/cancel")
    public ResponseEntity<ApiResponse<SlotResponse>> cancelSlot(
            @PathVariable Long slotId,
            @Valid @RequestBody CancelSlotRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = userService.findByEmail(userDetails.getUsername());
        log.info("Admin {} cancelling slot {}", admin.getEmail(), slotId);

        SlotResponse response = slotService.cancelSlot(slotId, request, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Slot cancelled", response));
    }
}