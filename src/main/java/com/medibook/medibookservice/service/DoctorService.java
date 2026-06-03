package com.medibook.medibookservice.service;

import com.medibook.medibookservice.dto.request.DoctorApprovalRequest;
import com.medibook.medibookservice.dto.request.DoctorProfileCreateRequest;
import com.medibook.medibookservice.dto.request.DoctorProfileUpdateRequest;
import com.medibook.medibookservice.dto.request.DoctorRejectionRequest;
import com.medibook.medibookservice.dto.response.DoctorPublicResponse;
import com.medibook.medibookservice.dto.response.DoctorResponse;
import com.medibook.medibookservice.dto.response.DoctorSummaryResponse;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.enums.DoctorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Doctor business operations.
 *
 * <p>Organized into three groups:</p>
 * <ul>
 *   <li>Doctor self-service (the logged-in doctor manages own profile)</li>
 *   <li>Admin operations (approval workflow, status changes)</li>
 *   <li>Public queries (anyone can browse APPROVED doctors)</li>
 * </ul>
 */
public interface DoctorService {

    // =========================================================
    // DOCTOR SELF-SERVICE (authenticated DOCTOR role)
    // =========================================================

    /**
     * Doctor creates their professional profile.
     * Status starts as PENDING_APPROVAL.
     *
     * @throws ResourceAlreadyExistsException if doctor already has a profile
     * @throws ResourceAlreadyExistsException if license number is taken
     */
    DoctorResponse createProfile(DoctorProfileCreateRequest request, User user);

    /**
     * Doctor updates their profile.
     * Only allowed when status is PENDING_APPROVAL or APPROVED.
     * If REJECTED with resubmission allowed, use resubmitProfile instead.
     */
    DoctorResponse updateMyProfile(DoctorProfileUpdateRequest request, User user);

    /**
     * Doctor views their own profile.
     */
    DoctorResponse getMyProfile(User user);

    /**
     * Doctor resubmits profile after rejection.
     * Only allowed if status=REJECTED AND resubmissionAllowed=true.
     * Resets status back to PENDING_APPROVAL.
     */
    DoctorResponse resubmitProfile(DoctorProfileUpdateRequest request, User user);

    // =========================================================
    // ADMIN OPERATIONS (authenticated ADMIN role)
    // =========================================================

    /**
     * Admin approves a doctor's profile.
     * Doctor must be in PENDING_APPROVAL status.
     * Sets status=APPROVED, records approver and timestamp.
     */
    DoctorResponse approveDoctor(Long doctorId, DoctorApprovalRequest request, User adminUser);

    /**
     * Admin rejects a doctor's profile.
     * Doctor must be in PENDING_APPROVAL status.
     * Records rejection reason and whether resubmission is allowed.
     */
    DoctorResponse rejectDoctor(Long doctorId, DoctorRejectionRequest request, User adminUser);

    /**
     * Admin suspends an approved doctor.
     * Doctor must be in APPROVED status.
     */
    DoctorResponse suspendDoctor(Long doctorId, User adminUser);

    /**
     * Admin reactivates a suspended doctor.
     * Doctor must be in SUSPENDED status.
     */
    DoctorResponse reactivateDoctor(Long doctorId, User adminUser);

    /**
     * Admin retrieves any doctor's full details.
     */
    DoctorResponse getDoctorByIdForAdmin(Long doctorId);

    /**
     * Admin retrieves doctors filtered by status (paginated).
     */
    Page<DoctorSummaryResponse> getDoctorsByStatus(DoctorStatus status, Pageable pageable);

    // =========================================================
    // PUBLIC QUERIES (no auth required)
    // =========================================================

    /**
     * Public listing of approved doctors (paginated).
     */
    Page<DoctorPublicResponse> getApprovedDoctors(Pageable pageable);

    /**
     * Public details of one approved doctor.
     * Returns 404 if doctor not approved (hide existence).
     */
    DoctorPublicResponse getApprovedDoctorById(Long doctorId);

    /**
     * Search approved doctors by name and/or specialization.
     * Both parameters are optional (null = no filter).
     */
    Page<DoctorPublicResponse> searchApprovedDoctors(
            String name, String specialization, Pageable pageable);

    /**
     * Get distinct list of specializations from APPROVED doctors.
     * Used to populate filter dropdowns on the frontend.
     */
    List<String> getDistinctSpecializations();
}