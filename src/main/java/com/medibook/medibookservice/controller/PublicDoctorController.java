package com.medibook.medibookservice.controller;

import com.medibook.medibookservice.dto.response.ApiResponse;
import com.medibook.medibookservice.dto.response.DoctorPublicResponse;
import com.medibook.medibookservice.service.DoctorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public controller for browsing doctors.
 * NO authentication required - anyone can view approved doctors.
 *
 * Base path: /api/v1/doctors
 *
 * Only APPROVED doctors are visible here.
 */
@RestController
@RequestMapping("/doctors")
public class PublicDoctorController {

    private final DoctorService doctorService;

    public PublicDoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * List approved doctors (paginated).
     *
     * Examples:
     *   GET /doctors                                  → page=0, size=10
     *   GET /doctors?page=1                           → page 1
     *   GET /doctors?sort=fullName,asc                → sort by name
     *   GET /doctors?sort=experienceYears,desc        → most experienced first
     *   GET /doctors?sort=consultationFee,asc         → cheapest first
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<DoctorPublicResponse>>> getApprovedDoctors(
            @PageableDefault(size = 10, sort = "fullName",
                    direction = Sort.Direction.ASC) Pageable pageable) {

        Page<DoctorPublicResponse> doctors = doctorService.getApprovedDoctors(pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Doctors retrieved", doctors));
    }

    /**
     * Get details of one approved doctor.
     * Returns 404 if doctor not found or not approved (privacy).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorPublicResponse>> getDoctorById(
            @PathVariable Long id) {

        DoctorPublicResponse response = doctorService.getApprovedDoctorById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Doctor details retrieved", response));
    }

    /**
     * Search approved doctors by name and/or specialization.
     *
     * Examples:
     *   GET /doctors/search?name=smith
     *   GET /doctors/search?specialization=Cardiology
     *   GET /doctors/search?name=smith&specialization=Cardiology
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<DoctorPublicResponse>>> searchDoctors(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialization,
            @PageableDefault(size = 10, sort = "fullName",
                    direction = Sort.Direction.ASC) Pageable pageable) {

        Page<DoctorPublicResponse> doctors =
                doctorService.searchApprovedDoctors(name, specialization, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Search results retrieved", doctors));
    }

    /**
     * Get distinct list of specializations (for filter dropdown).
     */
    @GetMapping("/specializations")
    public ResponseEntity<ApiResponse<List<String>>> getSpecializations() {
        List<String> specializations = doctorService.getDistinctSpecializations();
        return ResponseEntity.ok(
                ApiResponse.success("Specializations retrieved", specializations));
    }
}