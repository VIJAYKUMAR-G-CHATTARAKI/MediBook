package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.request.CancelBookingRequest;
import com.medibook.medibookservice.dto.request.CreateBookingRequest;
import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.BookingDetailWithHistoryResponse;
import com.medibook.medibookservice.dto.response.BookingHistoryResponse;
import com.medibook.medibookservice.dto.response.BookingResponse;
import com.medibook.medibookservice.dto.response.BookingSummaryResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.service.BookingHistoryService;
import com.medibook.medibookservice.service.BookingService;
import com.medibook.medibookservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Patient self-service booking endpoints.
 * Patients can create, view, and cancel their OWN bookings.
 */
@RestController
@RequestMapping("/me/bookings")
@PreAuthorize("hasRole('PATIENT')")
public class PatientBookingController {

    private static final Logger log = LoggerFactory.getLogger(PatientBookingController.class);

    private final BookingService bookingService;
    private final BookingHistoryService bookingHistoryService;
    private final UserService userService;

    public PatientBookingController(BookingService bookingService,
                                    BookingHistoryService bookingHistoryService,
                                    UserService userService) {
        this.bookingService = bookingService;
        this.bookingHistoryService = bookingHistoryService;
        this.userService = userService;
    }

    /**
     * Create a new booking for the logged-in patient.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User patient = userService.findByEmail(userDetails.getUsername());
        log.info("Patient {} creating booking for slot {}",
                patient.getEmail(), request.getSlotId());

        BookingResponse response = bookingService.createBooking(patient.getId(), request);
        return new ResponseEntity<>(
                ApiResponse.success("Booking created", response),
                HttpStatus.CREATED);
    }

    /**
     * List all bookings for the logged-in patient.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingSummaryResponse>>> getMyBookings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        User patient = userService.findByEmail(userDetails.getUsername());
        Page<BookingSummaryResponse> bookings = bookingService.getBookingsForPatient(
                patient.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", bookings));
    }

    /**
     * View a single booking's details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Verify ownership
        BookingResponse booking = bookingService.getBookingById(id);
        verifyOwnership(booking, userDetails);

        return ResponseEntity.ok(ApiResponse.success("Booking details", booking));
    }

    /**
     * View booking's history timeline.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<BookingDetailWithHistoryResponse>> getBookingHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse booking = bookingService.getBookingById(id);
        verifyOwnership(booking, userDetails);

        List<BookingHistoryResponse> history = bookingHistoryService.getBookingTimeline(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Booking with history",
                new BookingDetailWithHistoryResponse(booking, history)));
    }

    /**
     * Cancel own booking (enforces 2-hour rule).
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelMyBooking(
            @PathVariable Long id,
            @Valid @RequestBody CancelBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User patient = userService.findByEmail(userDetails.getUsername());

        // Verify ownership
        BookingResponse booking = bookingService.getBookingById(id);
        verifyOwnership(booking, userDetails);

        log.info("Patient {} cancelling booking {}", patient.getEmail(), id);

        BookingResponse response = bookingService.cancelBooking(
                id, request, patient.getId(), "PATIENT");
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", response));
    }

    /**
     * Look up booking by reference code.
     */
    @GetMapping("/reference/{reference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getByReference(
            @PathVariable String reference,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse booking = bookingService.getBookingByReference(reference);
        verifyOwnership(booking, userDetails);
        return ResponseEntity.ok(ApiResponse.success("Booking found", booking));
    }

    // =========================================================
    // PRIVATE HELPER
    // =========================================================

    /**
     * Verify the booking belongs to the logged-in patient.
     * Throws if patient tries to access another patient's booking.
     */
    private void verifyOwnership(BookingResponse booking, UserDetails userDetails) {
        User patient = userService.findByEmail(userDetails.getUsername());
        if (!booking.getPatientUserId().equals(patient.getId())) {
            throw new com.medibook.medibookservice.exception.ResourceNotFoundException(
                    "Booking not found");
        }
    }
}