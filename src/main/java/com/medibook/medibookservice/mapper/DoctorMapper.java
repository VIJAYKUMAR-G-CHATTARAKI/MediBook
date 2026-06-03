package com.medibook.medibookservice.mapper;

import com.medibook.medibookservice.dto.request.DoctorProfileCreateRequest;
import com.medibook.medibookservice.dto.request.DoctorProfileUpdateRequest;
import com.medibook.medibookservice.dto.response.DoctorPublicResponse;
import com.medibook.medibookservice.dto.response.DoctorResponse;
import com.medibook.medibookservice.dto.response.DoctorSummaryResponse;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Doctor entity and various DTOs.
 *
 * <p>Provides three response variants:
 * <ul>
 *   <li>{@link DoctorResponse} - Full details (for doctor and admin)</li>
 *   <li>{@link DoctorPublicResponse} - Public-safe (for patients browsing)</li>
 *   <li>{@link DoctorSummaryResponse} - Minimal (for list views)</li>
 * </ul>
 */
@Component
public class DoctorMapper {

    // =========================================================
    // DTO -> ENTITY (for creation)
    // =========================================================

    /**
     * Convert a creation request to a new Doctor entity.
     * The user must be set separately by the service.
     * Status defaults to PENDING_APPROVAL (from entity default).
     */
    public Doctor toEntity(DoctorProfileCreateRequest request, User user) {
        if (request == null) return null;

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setFullName(request.getFullName());
        doctor.setPhoneNumber(request.getPhoneNumber());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualification(request.getQualification());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setLicenseNumber(request.getLicenseNumber());
        doctor.setBio(request.getBio());
        doctor.setProfilePictureUrl(request.getProfilePictureUrl());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setLanguagesSpoken(request.getLanguagesSpoken());
        // Note: status defaults to PENDING_APPROVAL in entity
        return doctor;
    }

    // =========================================================
    // DTO -> ENTITY (for updates - modifies existing entity)
    // =========================================================

    /**
     * Apply update request to an existing Doctor entity.
     * Does NOT modify: id, user, licenseNumber, status, audit fields, timestamps.
     */
    public void updateEntity(Doctor doctor, DoctorProfileUpdateRequest request) {
        if (doctor == null || request == null) return;

        doctor.setFullName(request.getFullName());
        doctor.setPhoneNumber(request.getPhoneNumber());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setQualification(request.getQualification());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setBio(request.getBio());
        doctor.setProfilePictureUrl(request.getProfilePictureUrl());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setLanguagesSpoken(request.getLanguagesSpoken());
        // licenseNumber is intentionally NOT updated
    }

    // =========================================================
    // ENTITY -> DTO (full response, for doctor and admin)
    // =========================================================

    /**
     * Convert Doctor entity to full response DTO.
     * Includes all fields - use only for authenticated endpoints.
     */
    public DoctorResponse toResponse(Doctor doctor) {
        if (doctor == null) return null;

        DoctorResponse response = new DoctorResponse();
        response.setId(doctor.getId());

        // User-related (lazy-loaded, safe to access in service layer)
        if (doctor.getUser() != null) {
            response.setUserId(doctor.getUser().getId());
            response.setEmail(doctor.getUser().getEmail());
        }

        response.setFullName(doctor.getFullName());
        response.setPhoneNumber(doctor.getPhoneNumber());
        response.setSpecialization(doctor.getSpecialization());
        response.setQualification(doctor.getQualification());
        response.setExperienceYears(doctor.getExperienceYears());
        response.setLicenseNumber(doctor.getLicenseNumber());
        response.setBio(doctor.getBio());
        response.setProfilePictureUrl(doctor.getProfilePictureUrl());
        response.setConsultationFee(doctor.getConsultationFee());
        response.setLanguagesSpoken(doctor.getLanguagesSpoken());

        // Approval workflow
        response.setStatus(doctor.getStatus());
        response.setRejectionReason(doctor.getRejectionReason());
        response.setResubmissionAllowed(doctor.isResubmissionAllowed());

        // Audit trail
        response.setApprovedByUserId(doctor.getApprovedByUserId());
        response.setApprovedAt(doctor.getApprovedAt());
        response.setRejectedByUserId(doctor.getRejectedByUserId());
        response.setRejectedAt(doctor.getRejectedAt());

        // Timestamps
        response.setCreatedAt(doctor.getCreatedAt());
        response.setUpdatedAt(doctor.getUpdatedAt());

        return response;
    }

    // =========================================================
    // ENTITY -> DTO (public response, for patient-facing endpoints)
    // =========================================================

    /**
     * Convert Doctor entity to public-safe response DTO.
     * EXCLUDES: phone, license, status, audit info, timestamps.
     * Used for patient-facing listings.
     */
    public DoctorPublicResponse toPublicResponse(Doctor doctor) {
        if (doctor == null) return null;

        DoctorPublicResponse response = new DoctorPublicResponse();
        response.setId(doctor.getId());
        response.setFullName(doctor.getFullName());
        response.setSpecialization(doctor.getSpecialization());
        response.setQualification(doctor.getQualification());
        response.setExperienceYears(doctor.getExperienceYears());
        response.setBio(doctor.getBio());
        response.setProfilePictureUrl(doctor.getProfilePictureUrl());
        response.setConsultationFee(doctor.getConsultationFee());
        response.setLanguagesSpoken(doctor.getLanguagesSpoken());
        return response;
    }

    // =========================================================
    // ENTITY -> DTO (summary, for list views)
    // =========================================================

    /**
     * Convert Doctor entity to summary DTO for list views.
     * Includes status (useful for admin lists).
     * For public list views, set status to null after mapping.
     */
    public DoctorSummaryResponse toSummaryResponse(Doctor doctor) {
        if (doctor == null) return null;

        DoctorSummaryResponse response = new DoctorSummaryResponse();
        response.setId(doctor.getId());
        response.setFullName(doctor.getFullName());
        response.setSpecialization(doctor.getSpecialization());
        response.setExperienceYears(doctor.getExperienceYears());
        response.setConsultationFee(doctor.getConsultationFee());
        response.setProfilePictureUrl(doctor.getProfilePictureUrl());
        response.setStatus(doctor.getStatus());
        return response;
    }

    /**
     * Public list view - excludes status.
     */
    public DoctorSummaryResponse toPublicSummaryResponse(Doctor doctor) {
        DoctorSummaryResponse response = toSummaryResponse(doctor);
        if (response != null) {
            response.setStatus(null);  // Hide status from public
        }
        return response;
    }
}
