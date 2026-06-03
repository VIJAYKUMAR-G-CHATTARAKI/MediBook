package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.request.DoctorApprovalRequest;
import com.medibook.medibookservice.dto.request.DoctorRejectionRequest;
import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.DoctorResponse;
import com.medibook.medibookservice.dto.response.DoctorSummaryResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.enums.DoctorStatus;
import com.medibook.medibookservice.service.DoctorService;
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

/**
 * Admin controller for managing doctors.
 * All endpoints require ADMIN role.
 *
 * Base path: /api/v1/admin/doctors
 */
@RestController
@RequestMapping("/admin/doctors")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorController {

    private static final Logger log = LoggerFactory.getLogger(AdminDoctorController.class);

    private final DoctorService doctorService;
    private final UserService userService;

    public AdminDoctorController(DoctorService doctorService, UserService userService) {
        this.doctorService = doctorService;
        this.userService = userService;
    }

    /**
     * Get any doctor's full details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long id) {
        DoctorResponse response = doctorService.getDoctorByIdForAdmin(id);
        return ResponseEntity.ok(
                ApiResponse.success("Doctor details retrieved", response));
    }

    /**
     * List doctors filtered by status (paginated).
     * Default: status=PENDING_APPROVAL, sorted by createdAt descending.
     *
     * Example: GET /admin/doctors?status=PENDING_APPROVAL&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DoctorSummaryResponse>>> getDoctorsByStatus(
            @RequestParam(defaultValue = "PENDING_APPROVAL") DoctorStatus status,
            @PageableDefault(size = 10, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        Page<DoctorSummaryResponse> doctors = doctorService.getDoctorsByStatus(status, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Doctors retrieved", doctors));
    }

    /**
     * Approve a doctor's profile.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<DoctorResponse>> approveDoctor(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DoctorApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Approval request for doctor {} by admin {}", id, userDetails.getUsername());
        User adminUser = userService.findByEmail(userDetails.getUsername());

        // Body is optional - create empty if not provided
        if (request == null) {
            request = new DoctorApprovalRequest();
        }

        DoctorResponse response = doctorService.approveDoctor(id, request, adminUser);
        return ResponseEntity.ok(
                ApiResponse.success("Doctor approved successfully", response));
    }

    /**
     * Reject a doctor's profile (reason required).
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<DoctorResponse>> rejectDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorRejectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Rejection request for doctor {} by admin {}", id, userDetails.getUsername());
        User adminUser = userService.findByEmail(userDetails.getUsername());
        DoctorResponse response = doctorService.rejectDoctor(id, request, adminUser);

        return ResponseEntity.ok(
                ApiResponse.success("Doctor rejected", response));
    }

    /**
     * Suspend an approved doctor.
     */
    @PutMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<DoctorResponse>> suspendDoctor(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Suspension request for doctor {} by admin {}", id, userDetails.getUsername());
        User adminUser = userService.findByEmail(userDetails.getUsername());
        DoctorResponse response = doctorService.suspendDoctor(id, adminUser);

        return ResponseEntity.ok(
                ApiResponse.success("Doctor suspended", response));
    }

    /**
     * Reactivate a suspended doctor.
     */
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<DoctorResponse>> reactivateDoctor(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Reactivation request for doctor {} by admin {}", id, userDetails.getUsername());
        User adminUser = userService.findByEmail(userDetails.getUsername());
        DoctorResponse response = doctorService.reactivateDoctor(id, adminUser);

        return ResponseEntity.ok(
                ApiResponse.success("Doctor reactivated", response));
    }
}