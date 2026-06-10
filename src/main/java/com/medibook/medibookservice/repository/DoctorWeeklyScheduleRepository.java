package com.medibook.medibookservice.repository;

import com.medibook.medibookservice.entity.DoctorSlotConfig;
import com.medibook.medibookservice.entity.DoctorWeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DoctorWeeklySchedule - 7 entries per config (one per day).
 */
@Repository
public interface DoctorWeeklyScheduleRepository
        extends JpaRepository<DoctorWeeklySchedule, Long> {

    /**
     * Find all 7 weekly schedule entries for a config.
     */
    List<DoctorWeeklySchedule> findBySlotConfig(DoctorSlotConfig slotConfig);

    /**
     * Find schedule for a specific day under a specific config.
     */
    Optional<DoctorWeeklySchedule> findBySlotConfigAndDayOfWeek(
            DoctorSlotConfig slotConfig, DayOfWeek dayOfWeek);

    /**
     * Find all working schedules (excluding non-working days).
     */
    List<DoctorWeeklySchedule> findBySlotConfigAndIsWorkingDayTrue(
            DoctorSlotConfig slotConfig);

    /**
     * Delete all schedules for a config (used when reconfiguring).
     */
    void deleteBySlotConfig(DoctorSlotConfig slotConfig);
}