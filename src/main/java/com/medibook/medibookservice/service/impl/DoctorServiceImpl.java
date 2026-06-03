package com.medibook.medibookservice.service.impl;

import com.medibook.medibookservice.dto.request.DoctorApprovalRequest;
import com.medibook.medibookservice.dto.request.DoctorProfileCreateRequest;
import com.medibook.medibookservice.dto.request.DoctorProfileUpdateRequest;
import com.medibook.medibookservice.dto.request.DoctorRejectionRequest;
import com.medibook.medibookservice.dto.response.DoctorPublicResponse;
import com.medibook.medibookservice.dto.response.DoctorResponse;
import com.medibook.medibookservice.dto.response.DoctorSummaryResponse;
import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.enums.DoctorStatus;
import com.medibook.medibookservice.enums.Role;
import com.medibook.medibookservice.exception.BadRequestException;
import com.medibook.medibookservice.exception.ResourceAlreadyExistsException;
import com.medibook.medibookservice.exception.ResourceNotFoundException;
import com.medibook.medibookservice.mapper.DoctorMapper;
import com.medibook.medibookservice.repository.DoctorRepository;
import com.medibook.medibookservice.service.DoctorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorServiceImpl.class);

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;

    public DoctorServiceImpl(DoctorRepository doctorRepository, DoctorMapper doctorMapper) {
        this.doctorRepository = doctorRepository;
        this.doctorMapper = doctorMapper;
    }

    // =========================================================
    // DOCTOR SELF-SERVICE
    // =========================================================

    @Override
    @Transactional
    public DoctorResponse createProfile(DoctorProfileCreateRequest request, User user) {
        log.info("Doctor profile creation attempt by user: {}", user.getEmail());

        // Verify the user has DOCTOR role
        if (user.getRole() != Role.DOCTOR) {
            throw new BadRequestException("Only users with DOCTOR role can create a doctor profile");
        }

        // Prevent duplicate profile for same user
        if (doctorRepository.existsByUser(user)) {
            throw new ResourceAlreadyExistsException(
                    "Doctor profile already exists for this user. Use update instead.");
        }

        // Prevent duplicate license number
        if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new ResourceAlreadyExistsException(
                    "Doctor", "license number", request.getLicenseNumber());
        }

        Doctor doctor = doctorMapper.toEntity(request, user);
        Doctor saved = doctorRepository.save(doctor);

        log.info("Doctor profile created with id: {} for user: {}", saved.getId(), user.getEmail());
        return doctorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DoctorResponse updateMyProfile(DoctorProfileUpdateRequest request, User user) {
        log.info("Doctor profile update by user: {}", user.getEmail());

        Doctor doctor = findDoctorByUser(user);

        // Cannot update if rejected (must use resubmit)
        if (doctor.getStatus() == DoctorStatus.REJECTED) {
            throw new BadRequestException(
                    "Profile is rejected. Use the resubmit endpoint to update and re-apply.");
        }

        // Cannot update if suspended
        if (doctor.getStatus() == DoctorStatus.SUSPENDED) {
            throw new BadRequestException(
                    "Profile is suspended. Contact admin for reactivation.");
        }

        doctorMapper.updateEntity(doctor, request);
        // JPA dirty checking saves automatically

        log.info("Doctor profile updated for user: {}", user.getEmail());
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getMyProfile(User user) {
        Doctor doctor = findDoctorByUser(user);
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse resubmitProfile(DoctorProfileUpdateRequest request, User user) {
        log.info("Doctor profile resubmission by user: {}", user.getEmail());

        Doctor doctor = findDoctorByUser(user);

        // Only REJECTED doctors can resubmit
        if (doctor.getStatus() != DoctorStatus.REJECTED) {
            throw new BadRequestException(
                    "Resubmit is only allowed for REJECTED profiles. Current status: "
                            + doctor.getStatus());
        }

        // Check if resubmission was explicitly allowed by admin
        if (!doctor.isResubmissionAllowed()) {
            throw new BadRequestException(
                    "Resubmission is not allowed for this profile. Please contact admin.");
        }

        // Apply updates
        doctorMapper.updateEntity(doctor, request);

        // Reset to pending approval
        doctor.setStatus(DoctorStatus.PENDING_APPROVAL);
        doctor.setRejectionReason(null);
        doctor.setResubmissionAllowed(false);
        doctor.setRejectedByUserId(null);
        doctor.setRejectedAt(null);

        log.info("Doctor profile resubmitted, back to PENDING_APPROVAL for user: {}", user.getEmail());
        return doctorMapper.toResponse(doctor);
    }

    // =========================================================
    // ADMIN OPERATIONS
    // =========================================================

    @Override
    @Transactional
    public DoctorResponse approveDoctor(Long doctorId, DoctorApprovalRequest request, User adminUser) {
        log.info("Doctor approval by admin: {} for doctorId: {}", adminUser.getEmail(), doctorId);

        Doctor doctor = findDoctorById(doctorId);

        if (doctor.getStatus() != DoctorStatus.PENDING_APPROVAL) {
            throw new BadRequestException(
                    "Doctor can only be approved from PENDING_APPROVAL status. Current: "
                            + doctor.getStatus());
        }

        doctor.setStatus(DoctorStatus.APPROVED);
        doctor.setApprovedByUserId(adminUser.getId());
        doctor.setApprovedAt(LocalDateTime.now());

        // Clear any previous rejection info
        doctor.setRejectionReason(null);
        doctor.setResubmissionAllowed(false);
        doctor.setRejectedByUserId(null);
        doctor.setRejectedAt(null);

        log.info("Doctor approved with id: {}", doctorId);
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse rejectDoctor(Long doctorId, DoctorRejectionRequest request, User adminUser) {
        log.info("Doctor rejection by admin: {} for doctorId: {}", adminUser.getEmail(), doctorId);

        Doctor doctor = findDoctorById(doctorId);

        if (doctor.getStatus() != DoctorStatus.PENDING_APPROVAL) {
            throw new BadRequestException(
                    "Doctor can only be rejected from PENDING_APPROVAL status. Current: "
                            + doctor.getStatus());
        }

        doctor.setStatus(DoctorStatus.REJECTED);
        doctor.setRejectionReason(request.getRejectionReason());
        doctor.setResubmissionAllowed(Boolean.TRUE.equals(request.getResubmissionAllowed()));
        doctor.setRejectedByUserId(adminUser.getId());
        doctor.setRejectedAt(LocalDateTime.now());

        log.info("Doctor rejected with id: {}, resubmission allowed: {}",
                doctorId, doctor.isResubmissionAllowed());
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse suspendDoctor(Long doctorId, User adminUser) {
        log.info("Doctor suspension by admin: {} for doctorId: {}", adminUser.getEmail(), doctorId);

        Doctor doctor = findDoctorById(doctorId);

        if (doctor.getStatus() != DoctorStatus.APPROVED) {
            throw new BadRequestException(
                    "Only APPROVED doctors can be suspended. Current status: " + doctor.getStatus());
        }

        doctor.setStatus(DoctorStatus.SUSPENDED);
        log.info("Doctor suspended with id: {}", doctorId);
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse reactivateDoctor(Long doctorId, User adminUser) {
        log.info("Doctor reactivation by admin: {} for doctorId: {}", adminUser.getEmail(), doctorId);

        Doctor doctor = findDoctorById(doctorId);

        if (doctor.getStatus() != DoctorStatus.SUSPENDED) {
            throw new BadRequestException(
                    "Only SUSPENDED doctors can be reactivated. Current status: " + doctor.getStatus());
        }

        doctor.setStatus(DoctorStatus.APPROVED);
        log.info("Doctor reactivated with id: {}", doctorId);
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorByIdForAdmin(Long doctorId) {
        Doctor doctor = findDoctorById(doctorId);
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorSummaryResponse> getDoctorsByStatus(DoctorStatus status, Pageable pageable) {
        return doctorRepository.findByStatus(status, pageable)
                .map(doctorMapper::toSummaryResponse);
    }

    // =========================================================
    // PUBLIC QUERIES
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorPublicResponse> getApprovedDoctors(Pageable pageable) {
        return doctorRepository.findByStatus(DoctorStatus.APPROVED, pageable)
                .map(doctorMapper::toPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorPublicResponse getApprovedDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));

        // If not approved, pretend they don't exist (privacy + clarity)
        if (doctor.getStatus() != DoctorStatus.APPROVED) {
            throw new ResourceNotFoundException("Doctor", "id", doctorId);
        }

        return doctorMapper.toPublicResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorPublicResponse> searchApprovedDoctors(
            String name, String specialization, Pageable pageable) {

        Page<Doctor> doctors;

        boolean hasName = name != null && !name.isBlank();
        boolean hasSpec = specialization != null && !specialization.isBlank();

        if (hasName) {
            doctors = doctorRepository.searchApprovedDoctorsByName(
                    DoctorStatus.APPROVED, name.trim(), pageable);
        } else if (hasSpec) {
            doctors = doctorRepository.findByStatusAndSpecialization(
                    DoctorStatus.APPROVED, specialization.trim(), pageable);
        } else {
            doctors = doctorRepository.findByStatus(DoctorStatus.APPROVED, pageable);
        }

        return doctors.map(doctorMapper::toPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctSpecializations() {
        return doctorRepository.findDistinctSpecializationsByStatus(DoctorStatus.APPROVED);
    }

    // =========================================================
    // HELPER METHODS (private)
    // =========================================================

    private Doctor findDoctorByUser(User user) {
        return doctorRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor profile not found for user: " + user.getEmail()));
    }

    private Doctor findDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", doctorId));
    }
}