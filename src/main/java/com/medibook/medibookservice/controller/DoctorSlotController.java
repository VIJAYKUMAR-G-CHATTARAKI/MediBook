package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.request.CancelSlotRequest;
import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.SlotResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.repository.DoctorRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Doctor self-service slot endpoints.
 * Doctor views and cancels their OWN slots.
 */
@RestController
@RequestMapping("/doctors/me/slots")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorSlotController {

    private static final Logger log = LoggerFactory.getLogger(DoctorSlotController.class);

    private final DoctorSlotService doctorSlotService;
    private final UserService userService;
    private final DoctorRepository doctorRepository;

    public DoctorSlotController(DoctorSlotService doctorSlotService,
                                UserService userService,
                                DoctorRepository doctorRepository) {
        this.doctorSlotService = doctorSlotService;
        this.userService = userService;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Get logged-in doctor's slots in a date range.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SlotResponse>>> getMySlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "slotDate",
                    direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long doctorId = getDoctorIdForUser(userDetails);
        Page<SlotResponse> slots = doctorSlotService.getSlotsForDoctorInRange(
                doctorId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Slots retrieved", slots));
    }

    /**
     * Get a specific slot detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SlotResponse>> getSlotDetail(@PathVariable Long id) {
        SlotResponse slot = doctorSlotService.getSlotById(id);
        return ResponseEntity.ok(ApiResponse.success("Slot details retrieved", slot));
    }

    /**
     * Cancel one of doctor's own slots.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SlotResponse>> cancelMySlot(
            @PathVariable Long id,
            @Valid @RequestBody CancelSlotRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        log.info("Doctor {} cancelling slot {}", user.getEmail(), id);

        SlotResponse slot = doctorSlotService.cancelSlot(id, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Slot cancelled", slot));
    }

    // =========================================================
    // PRIVATE HELPER
    // =========================================================

    private Long getDoctorIdForUser(UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return doctorRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException(
                        "Doctor profile not found for user: " + user.getEmail()))
                .getId();
    }
}