package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.request.CancelBookingRequest;
import com.medibook.medibookservice.dto.request.CompleteBookingRequest;
import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.BookingDetailWithHistoryResponse;
import com.medibook.medibookservice.dto.response.BookingHistoryResponse;
import com.medibook.medibookservice.dto.response.BookingResponse;
import com.medibook.medibookservice.dto.response.BookingSummaryResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.repository.DoctorRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Doctor self-service booking endpoints.
 * Doctors can view, cancel, complete, and mark no-show bookings on their own slots.
 */
@RestController
@RequestMapping("/doctors/me/bookings")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorBookingController {

    private static final Logger log = LoggerFactory.getLogger(DoctorBookingController.class);

    private final BookingService bookingService;
    private final BookingHistoryService bookingHistoryService;
    private final UserService userService;
    private final DoctorRepository doctorRepository;

    public DoctorBookingController(BookingService bookingService,
                                   BookingHistoryService bookingHistoryService,
                                   UserService userService,
                                   DoctorRepository doctorRepository) {
        this.bookingService = bookingService;
        this.bookingHistoryService = bookingHistoryService;
        this.userService = userService;
        this.doctorRepository = doctorRepository;
    }

    /**
     * List all bookings for the doctor's own slots.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingSummaryResponse>>> getMyBookings(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long doctorId = getDoctorId(userDetails);
        Page<BookingSummaryResponse> bookings = bookingService.getBookingsForDoctor(
                doctorId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", bookings));
    }

    /**
     * View booking detail.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse booking = bookingService.getBookingById(id);
        verifyDoctorOwnership(booking, userDetails);
        return ResponseEntity.ok(ApiResponse.success("Booking details", booking));
    }

    /**
     * View booking timeline.
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<BookingDetailWithHistoryResponse>> getBookingHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse booking = bookingService.getBookingById(id);
        verifyDoctorOwnership(booking, userDetails);

        List<BookingHistoryResponse> history = bookingHistoryService.getBookingTimeline(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Booking with history",
                new BookingDetailWithHistoryResponse(booking, history)));
    }

    /**
     * Cancel a booking (doctor bypasses the 2-hour rule).
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody CancelBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse booking = bookingService.getBookingById(id);
        verifyDoctorOwnership(booking, userDetails);

        User doctorUser = userService.findByEmail(userDetails.getUsername());
        log.info("Doctor {} cancelling booking {}", doctorUser.getEmail(), id);

        BookingResponse response = bookingService.cancelBooking(
                id, request, doctorUser.getId(), "DOCTOR");
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", response));
    }

    /**
     * Mark a booking as COMPLETED.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(
            @PathVariable Long id,
            @RequestBody(required = false) CompleteBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse booking = bookingService.getBookingById(id);
        verifyDoctorOwnership(booking, userDetails);

        User doctorUser = userService.findByEmail(userDetails.getUsername());
        log.info("Doctor {} completing booking {}", doctorUser.getEmail(), id);

        BookingResponse response = bookingService.completeBooking(
                id, request, doctorUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking completed", response));
    }

    /**
     * Mark a booking as NO_SHOW.
     */
    @PostMapping("/{id}/no-show")
    public ResponseEntity<ApiResponse<BookingResponse>> markNoShow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        BookingResponse booking = bookingService.getBookingById(id);
        verifyDoctorOwnership(booking, userDetails);

        User doctorUser = userService.findByEmail(userDetails.getUsername());
        log.info("Doctor {} marking booking {} as NO_SHOW", doctorUser.getEmail(), id);

        BookingResponse response = bookingService.markNoShow(id, doctorUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking marked as no-show", response));
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private Long getDoctorId(UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return doctorRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException(
                        "Doctor profile not found"))
                .getId();
    }

    /**
     * Verify the booking is on one of the doctor's slots.
     */
    private void verifyDoctorOwnership(BookingResponse booking, UserDetails userDetails) {
        Long doctorId = getDoctorId(userDetails);
        if (!booking.getDoctorId().equals(doctorId)) {
            throw new com.medibook.medibookservice.exception.ResourceNotFoundException(
                    "Booking not found");
        }
    }
}