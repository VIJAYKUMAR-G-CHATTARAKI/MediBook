package com.medibook.medibookservice.repository;

import com.medibook.medibookservice.entity.Doctor;
import com.medibook.medibookservice.entity.DoctorSlotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for DoctorSlotConfig - one config per doctor.
 */
@Repository
public interface DoctorSlotConfigRepository extends JpaRepository<DoctorSlotConfig, Long> {

    /**
     * Find slot configuration for a specific doctor.
     */
    Optional<DoctorSlotConfig> findByDoctor(Doctor doctor);

    /**
     * Find slot configuration by doctor ID.
     */
    Optional<DoctorSlotConfig> findByDoctorId(Long doctorId);

    /**
     * Check if a doctor already has a slot configuration.
     */
    boolean existsByDoctor(Doctor doctor);
}