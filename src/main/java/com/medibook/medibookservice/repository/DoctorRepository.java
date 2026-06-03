package com.medibook.medibookservice.repository;

import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.User;
import com.medibook.medibookservice.enums.DoctorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Doctor entity providing data access operations.
 *
 * <p>Extends:
 * <ul>
 *   <li>{@link JpaRepository} - Basic CRUD operations</li>
 *   <li>{@link JpaSpecificationExecutor} - Dynamic queries via Specifications</li>
 * </ul>
 *
 * <p>Spring Data JPA automatically implements this interface at runtime.
 * No need to write SQL for the methods below - just declare the signatures.</p>
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long>,
        JpaSpecificationExecutor<Doctor> {

    // =========================================================
    // FIND BY USER (for logged-in doctor's profile)
    // =========================================================

    /**
     * Find a doctor profile by the linked User account.
     * Used when a logged-in doctor accesses /doctors/me endpoint.
     */
    Optional<Doctor> findByUser(User user);

    /**
     * Find a doctor profile by user ID (avoids loading full User).
     */
    Optional<Doctor> findByUserId(Long userId);

    // =========================================================
    // EXISTENCE CHECKS (for validation)
    // =========================================================

    /**
     * Check if a doctor profile exists for a given user.
     * Used to prevent a doctor from creating multiple profiles.
     */
    boolean existsByUser(User user);

    /**
     * Check if a license number is already registered.
     * Used during profile creation to prevent duplicates.
     */
    boolean existsByLicenseNumber(String licenseNumber);

    // =========================================================
    // QUERIES BY STATUS (admin workflows)
    // =========================================================

    /**
     * Find all doctors with a specific status, paginated.
     * Examples:
     *   - Admin views PENDING_APPROVAL doctors
     *   - Admin views REJECTED doctors
     *   - Admin views SUSPENDED doctors
     */
    Page<Doctor> findByStatus(DoctorStatus status, Pageable pageable);

    /**
     * Count doctors with a specific status.
     * Useful for admin dashboard statistics.
     */
    long countByStatus(DoctorStatus status);

    // =========================================================
    // PUBLIC QUERIES (only APPROVED doctors visible)
    // =========================================================

    /**
     * Find approved doctors by specialization.
     * Public listing - patients searching by specialty.
     */
    Page<Doctor> findByStatusAndSpecialization(
            DoctorStatus status,
            String specialization,
            Pageable pageable
    );

    /**
     * Search approved doctors by partial name match.
     * Case-insensitive search.
     */
    @Query("SELECT d FROM Doctor d " +
            "WHERE d.status = :status " +
            "AND LOWER(d.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Doctor> searchApprovedDoctorsByName(
            @Param("status") DoctorStatus status,
            @Param("name") String name,
            Pageable pageable
    );

    /**
     * Get distinct list of specializations (for filter dropdown).
     * Only from APPROVED doctors.
     */
    @Query("SELECT DISTINCT d.specialization FROM Doctor d " +
            "WHERE d.status = :status " +
            "ORDER BY d.specialization ASC")
    List<String> findDistinctSpecializationsByStatus(@Param("status") DoctorStatus status);
}