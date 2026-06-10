package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.request.CancelBookingRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for managing any booking in the system.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private static final Logger log = LoggerFactory.getLogger(AdminBookingController.class);

    private final BookingService bookingService;
    private final BookingHistoryService bookingHistoryService;
    private final UserService userService;

    public AdminBookingController(BookingService bookingService,
                                  BookingHistoryService bookingHistoryService,
                                  UserService userService) {
        this.bookingService = bookingService;
        this.bookingHistoryService = bookingHistoryService;
        this.userService = userService;
    }

    /**
     * Get any booking by ID.
     */
    @GetMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.success("Booking details", response));
    }

    /**
     * Get any booking by reference code.
     */
    @GetMapping("/bookings/reference/{reference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getByReference(
            @PathVariable String reference) {
        BookingResponse response = bookingService.getBookingByReference(reference);
        return ResponseEntity.ok(ApiResponse.success("Booking found", response));
    }

    /**
     * Get booking timeline.
     */
    @GetMapping("/bookings/{id}/history")
    public ResponseEntity<ApiResponse<BookingDetailWithHistoryResponse>> getBookingHistory(
            @PathVariable Long id) {

        BookingResponse booking = bookingService.getBookingById(id);
        List<BookingHistoryResponse> history = bookingHistoryService.getBookingTimeline(id);
        return ResponseEntity.ok(ApiResponse.success(
                "Booking with history",
                new BookingDetailWithHistoryResponse(booking, history)));
    }

    /**
     * Get any patient's bookings.
     */
    @GetMapping("/patients/{patientId}/bookings")
    public ResponseEntity<ApiResponse<Page<BookingSummaryResponse>>> getPatientBookings(
            @PathVariable Long patientId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<BookingSummaryResponse> bookings = bookingService.getBookingsForPatient(
                patientId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Patient bookings", bookings));
    }

    /**
     * Get any doctor's bookings.
     */
    @GetMapping("/doctors/{doctorId}/bookings")
    public ResponseEntity<ApiResponse<Page<BookingSummaryResponse>>> getDoctorBookings(
            @PathVariable Long doctorId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<BookingSummaryResponse> bookings = bookingService.getBookingsForDoctor(
                doctorId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Doctor bookings", bookings));
    }

    /**
     * Admin override cancellation (bypasses 2-hour rule).
     */
    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> adminCancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User admin = userService.findByEmail(userDetails.getUsername());
        log.info("Admin {} cancelling booking {}", admin.getEmail(), id);

        BookingResponse response = bookingService.cancelBooking(
                id, request, admin.getId(), "ADMIN");
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled by admin", response));
    }
}