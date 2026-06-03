package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.request.DoctorProfileCreateRequest;
import com.medibook.medibookservice.dto.request.DoctorProfileUpdateRequest;
import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.DoctorResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.service.DoctorService;
import com.medibook.medibookservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authenticated DOCTOR endpoints.
 * Doctors manage their OWN profile through these endpoints.
 *
 * Base path: /api/v1/doctors/me (from SecurityConfig context-path)
 */
@RestController
@RequestMapping("/doctors/me")
@PreAuthorize("hasRole('DOCTOR')")    // Class-level: ALL methods require DOCTOR role
public class DoctorController {

    private static final Logger log = LoggerFactory.getLogger(DoctorController.class);

    private final DoctorService doctorService;
    private final UserService userService;

    public DoctorController(DoctorService doctorService, UserService userService) {
        this.doctorService = doctorService;
        this.userService = userService;
    }

    /**
     * Doctor creates their professional profile.
     * Initial status: PENDING_APPROVAL
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DoctorResponse>> createProfile(
            @Valid @RequestBody DoctorProfileCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Profile creation request from: {}", userDetails.getUsername());
        User user = userService.findByEmail(userDetails.getUsername());
        DoctorResponse response = doctorService.createProfile(request, user);

        return new ResponseEntity<>(
                ApiResponse.success("Doctor profile created. Awaiting admin approval.", response),
                HttpStatus.CREATED);
    }

    /**
     * Doctor views their own profile.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DoctorResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userService.findByEmail(userDetails.getUsername());
        DoctorResponse response = doctorService.getMyProfile(user);

        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully", response));
    }

    /**
     * Doctor updates their profile.
     * Allowed only when status is PENDING_APPROVAL or APPROVED.
     */
    @PutMapping
    public ResponseEntity<ApiResponse<DoctorResponse>> updateMyProfile(
            @Valid @RequestBody DoctorProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Profile update request from: {}", userDetails.getUsername());
        User user = userService.findByEmail(userDetails.getUsername());
        DoctorResponse response = doctorService.updateMyProfile(request, user);

        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully", response));
    }

    /**
     * Doctor resubmits profile after rejection (only if allowed).
     */
    @PutMapping("/resubmit")
    public ResponseEntity<ApiResponse<DoctorResponse>> resubmitProfile(
            @Valid @RequestBody DoctorProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.info("Profile resubmission from: {}", userDetails.getUsername());
        User user = userService.findByEmail(userDetails.getUsername());
        DoctorResponse response = doctorService.resubmitProfile(request, user);

        return ResponseEntity.ok(
                ApiResponse.success("Profile resubmitted. Awaiting admin approval.", response));
    }
}